# USB Android Touchpad — Application Protocol

This document specifies the **application-layer binary protocol** carried over TCP between the Android client and the Windows desktop server. It is designed for **low latency**, **deterministic parsing**, and **TCP stream framing** (messages are not aligned with TCP segment boundaries).

## Design goals

- Compact binary encoding (no JSON or text commands)
- Fixed header for framing and version negotiation
- Big-endian (network byte order) multi-byte fields
- Strict payload lengths per message type
- Resynchronization on invalid magic bytes

## Transport

- **Protocol**: TCP (`SOCK_STREAM`)
- **Default port**: `5000` (configurable)
- **Typical path**: Android `Socket("127.0.0.1", 5000)` → `adb reverse tcp:5000 tcp:5000` → Windows `ServerSocket` on `127.0.0.1:5000`
- **Performance**: Both sides should set `Socket.setTcpNoDelay(true)` to disable Nagle’s algorithm for small packets.

## Frame layout

Every message is a single **frame**:

| Offset | Size | Field            | Description                          |
|--------|------|------------------|--------------------------------------|
| 0      | 1    | Magic0           | Must be `0x54` (`'T'`)               |
| 1      | 1    | Magic1           | Must be `0x50` (`'P'`)               |
| 2      | 1    | Version          | Currently `0x01`                     |
| 3      | 1    | Type             | Message type (see below)             |
| 4      | 2    | PayloadLength    | Unsigned int16, big-endian           |
| 6      | N    | Payload          | `N = PayloadLength` bytes            |

**Minimum frame size**: 6 bytes (empty payload).

**Maximum payload**: 64 bytes (reject larger lengths).

### Endianness

All multi-byte integers use **big-endian** (most significant byte first).

## Message types

| Type (hex) | Name               | Payload length | Payload format                                      |
|------------|--------------------|----------------|-----------------------------------------------------|
| `0x01`     | MOVE               | 4              | `dx`: int16 BE, `dy`: int16 BE (signed)             |
| `0x02`     | LEFT_CLICK         | 0              | —                                                   |
| `0x03`     | RIGHT_CLICK        | 0              | —                                                   |
| `0x04`     | SCROLL             | 2              | `deltaY`: int16 BE (signed wheel units)             |
| `0x05`     | LEFT_DOWN          | 0              | —                                                   |
| `0x06`     | LEFT_UP            | 0              | —                                                   |
| `0x07`     | PING               | 0              | —                                                   |
| `0x08`     | PONG               | 0              | — (server reply to PING)                            |
| `0x09`     | HORIZONTAL_SCROLL  | 2              | `deltaX`: int16 BE (signed; mapped to wheel on PC) |

Unknown types or length mismatches must be **rejected** without crashing the peer. The decoder skips bytes until the next valid magic `TP`.

## Semantics

### MOVE (`0x01`)

- Relative cursor motion in **desktop pixel units** (after client-side sensitivity scaling).
- `dx`, `dy` are signed 16-bit integers, clamped on encode to `[−32768, 32767]`.
- Server applies: `cursor += (dx, dy)` clamped to the virtual desktop bounds, then `Robot.mouseMove`.

### Clicks

- **LEFT_CLICK**: press and release button 1.
- **RIGHT_CLICK**: press and release button 3.
- **LEFT_DOWN / LEFT_UP**: used for drag; server must release buttons on disconnect.

### SCROLL (`0x04` / `0x09`)

- Client sends accumulated scroll deltas as int16 steps.
- Server may keep a fractional accumulator before calling `Robot.mouseWheel`.

### Heartbeat

- Client may send **PING** every few seconds when idle.
- Server responds with **PONG** (same framing, type `0x08`, empty payload).

## TCP stream framing (receiver algorithm)

TCP delivers a **byte stream**, not messages. The receiver:

1. Appends all bytes from `read()` into a buffer.
2. Scans for magic `TP` at the read cursor.
3. If fewer than 6 bytes available, wait for more data.
4. Parse version, type, and payload length.
5. If fewer than `6 + payloadLength` bytes available, wait.
6. Validate version, type, and exact payload length for that type.
7. Emit one logical packet and advance the cursor by the full frame length.
8. On invalid header/version/length, advance one byte and resync.

This handles:

- One frame per read
- Multiple frames per read
- One frame split across reads
- Garbage bytes before a valid frame

## Examples (hex)

**MOVE dx=12, dy=-7**

```
54 50 01 01 00 04 00 0C FF F9
```

**LEFT_CLICK**

```
54 50 01 02 00 00
```

**PING**

```
54 50 01 07 00 00
```

## Error handling

| Condition              | Client behavior              | Server behavior                |
|------------------------|------------------------------|--------------------------------|
| TCP reset              | Reconnect with backoff       | Release mouse buttons          |
| Invalid frame          | Continue sending valid frames| Skip/resync, count invalid     |
| Partial read           | N/A                          | Buffer until complete frame    |
| Oversized payload len  | N/A                          | Resync (skip magic search)     |

## Versioning

Increment `Version` for incompatible changes. Receivers must reject unknown versions and resync.
