package pvz.libpvz.pam;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PamBinaryReader {
    private static final long MAGIC = 0xBAF01954L;
    private static final int MAX_VERSION = 6;
    private final byte[] b;
    private int p;
    private PamBinaryReader(byte[] bytes) {
        this.b = bytes;
    }
    public static PamData parse(byte[] bytes) {
        return new PamBinaryReader(bytes).read();
    }
    private PamData read() {
        long magic = u32();
        if (magic != MAGIC) {
            throw new IllegalArgumentException(String.format("Not a PAM file (magic 0x%08X)", magic));
        }
        int version = i32();
        if (version > MAX_VERSION) {
            throw new IllegalArgumentException("Unsupported PAM version: " + version);
        }
        float frameRate = u8();
        float[] position = { i16() / 20f, i16() / 20f };
        float[] size = { i16() / 20f, i16() / 20f };
        int imageCount = i16();
        List<PamData.Image> images = new ArrayList<>(Math.max(0, imageCount));
        for (int i = 0; i < imageCount; i++) {
            images.add(readImage(version));
        }
        int spriteCount = i16();
        List<PamData.Sprite> sprites = new ArrayList<>(Math.max(0, spriteCount));
        for (int i = 0; i < spriteCount; i++) {
            sprites.add(readSprite(version, frameRate));
        }
        PamData.Sprite mainSprite = null;
        if (version <= 3 || readBoolean()) {
            mainSprite = readSprite(version, frameRate);
        }
        return new PamData(version, frameRate, position, size, images, sprites, mainSprite);
    }
    private PamData.Image readImage(int version) {
        String name = string();
        int[] size = { -1, -1 };
        if (version >= 4) {
            size[0] = i16();
            size[1] = i16();
        }
        float[] transform = new float[6];
        if (version == 1) {
            double r = i16() / 1000d;
            transform[0] = (float) Math.cos(r);
            transform[2] = (float) -Math.sin(r);
            transform[1] = (float) Math.sin(r);
            transform[3] = (float) Math.cos(r);
            transform[4] = i16() / 20f;
            transform[5] = i16() / 20f;
        } else {
            transform[0] = (float) (i32() / 1310720d); 
            transform[2] = (float) (i32() / 1310720d); 
            transform[1] = (float) (i32() / 1310720d); 
            transform[3] = (float) (i32() / 1310720d); 
            transform[4] = i16() / 20f;                
            transform[5] = i16() / 20f;                
        }
        return new PamData.Image(name, resourceId(name), size, transform);
    }
    private PamData.Sprite readSprite(int version, float globalFrameRate) {
        String name = null;
        String description = null;
        float frameRate = globalFrameRate;
        if (version >= 4) {
            name = string();
            if (version >= 6) {
                description = string();
            }
            frameRate = (float) (i32() / 65536d);
        }
        int frameCount = i16();
        int[] workArea = new int[2];
        if (version >= 5) {
            workArea[0] = i16();
            workArea[1] = i16();
        } else {
            workArea[0] = 0;
            workArea[1] = frameCount - 1;
        }
        workArea[1] = frameCount; 
        List<PamData.Frame> frames = new ArrayList<>(Math.max(0, frameCount));
        for (int i = 0; i < frameCount; i++) {
            frames.add(readFrame(version));
        }
        return new PamData.Sprite(name, description, frameRate, workArea, frames);
    }
    private PamData.Frame readFrame(int version) {
        int flags = u8();
        List<PamData.Remove> removes = new ArrayList<>();
        if ((flags & 0x01) != 0) {
            for (int i = 0, n = count(); i < n; i++) {
                int index = i16();
                if (index >= 2047) {
                    index = i32();
                }
                removes.add(new PamData.Remove(index));
            }
        }
        List<PamData.Add> appends = new ArrayList<>();
        if ((flags & 0x02) != 0) {
            for (int i = 0, n = count(); i < n; i++) {
                appends.add(readAdd(version));
            }
        }
        List<PamData.Move> changes = new ArrayList<>();
        if ((flags & 0x04) != 0) {
            for (int i = 0, n = count(); i < n; i++) {
                changes.add(readMove(version));
            }
        }
        String clip = null;
        if ((flags & 0x08) != 0) {
            clip = string();
        }
        boolean stop = (flags & 0x10) != 0;
        List<PamData.Command> commands = new ArrayList<>();
        if ((flags & 0x20) != 0) {
            for (int i = 0, n = u8(); i < n; i++) {
                commands.add(new PamData.Command(string(), string()));
            }
        }
        return new PamData.Frame(clip, stop, commands, removes, appends, changes);
    }
    private PamData.Add readAdd(int version) {
        int flags = u16();
        int index = flags & 2047;
        if (index == 2047) {
            index = i32();
        }
        boolean sprite = (flags & 32768) != 0;
        boolean additive = (flags & 16384) != 0;
        int resource = u8();
        if (version >= 6 && resource == 255) {
            resource = i16();
        }
        int preloadFrames = 0;
        if ((flags & 8192) != 0) {
            preloadFrames = i16();
        }
        String name = null;
        if ((flags & 4096) != 0) {
            name = string();
        }
        float timescale = 1f;
        if ((flags & 2048) != 0) {
            timescale = i32() / 65536f;
        }
        return new PamData.Add(index, name, resource, sprite, additive, preloadFrames, timescale);
    }
    private static final int M_SRC_RECT = 32768;
    private static final int M_ROTATE = 16384;
    private static final int M_COLOR = 8192;
    private static final int M_MATRIX = 4096;
    private static final int M_LONG_COORDS = 2048;
    private static final int M_ANIM_FRAME = 1024;
    private PamData.Move readMove(int version) {
        int flags = u16();
        int index = flags & 1023;
        if (index == 1023) {
            index = i32();
        }
        float[] transform;
        if ((flags & M_MATRIX) != 0) {
            transform = new float[6];
            transform[0] = (float) (i32() / 65536d); 
            transform[2] = (float) (i32() / 65536d); 
            transform[1] = (float) (i32() / 65536d); 
            transform[3] = (float) (i32() / 65536d); 
        } else if ((flags & M_ROTATE) != 0) {
            transform = new float[3];
            transform[0] = i16() / 1000f;
        } else {
            transform = new float[2];
        }
        if ((flags & M_LONG_COORDS) != 0) {
            transform[transform.length - 2] = i32() / 20f;
            transform[transform.length - 1] = i32() / 20f;
        } else {
            transform[transform.length - 2] = i16() / 20f;
            transform[transform.length - 1] = i16() / 20f;
        }
        int[] srcRect = null;
        if ((flags & M_SRC_RECT) != 0) {
            srcRect = new int[] { i16() / 20, i16() / 20, i16() / 20, i16() / 20 };
        }
        float[] color = null;
        if ((flags & M_COLOR) != 0) {
            color = new float[] { u8() / 255f, u8() / 255f, u8() / 255f, u8() / 255f };
        }
        int animFrameNum = 0;
        if ((flags & M_ANIM_FRAME) != 0) {
            animFrameNum = i16();
        }
        return new PamData.Move(index, transform, color, srcRect, animFrameNum);
    }
    private int count() {
        int n = u8();
        if (n == 255) {
            n = i16();
        }
        return n;
    }

    private static String resourceId(String name) {
        if (name == null) {
            return "";
        }
        int pipe = name.indexOf('|');
        return pipe >= 0 ? name.substring(pipe + 1) : name;
    }
    
    private int u8() {
        return b[p++] & 0xFF;
    }
    private boolean readBoolean() {
        return u8() != 0;
    }
    private int u16() {
        int v = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
        p += 2;
        return v;
    }
    private short i16() {
        return (short) u16();
    }
    private int i32() {
        int v = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
        p += 4;
        return v;
    }
    private long u32() {
        return i32() & 0xFFFFFFFFL;
    }
    private String string() {
        int len = u16();
        String s = new String(b, p, len, StandardCharsets.UTF_8);
        p += len;
        return s;
    }
}