package pvz.libpvz.pam;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import pvz.libpvz.Matrix2D;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Pre-computes and bakes hierarchical animation data into flat arrays of rendering commands.
 * This eliminates runtime matrix multiplications and hierarchy traversals during rendering,
 * ensuring extreme performance when drawing thousands of PAMs.
 */
final class BakedAnimation {
    /**
     * Represents a single node in the animation hierarchy.
     * Optimizations: Bitmask flags are pre-computed at load time for O(1) filtering (e.g. hiding 'armor').
     * The hierarchy is flattened so drawing loops simply iterate linearly over active parts.
     */
    public static class Part {
        public static final int FLAG_BUTTER              = 1 << 0;
        public static final int FLAG_INK                 = 1 << 1;
        public static final int FLAG_ARMOR               = 1 << 2;
        public static final int FLAG_CUSTOM              = 1 << 3;
        public static final int FLAG_GROUND_SWATCH       = 1 << 4;
        public static final int FLAG_GROUND_SWATCH_PLANE = 1 << 5;
        public static final int FLAG_ARM_UPPER_BONE      = 1 << 6;
        
        public final int id;
        public final String name;
        public final Part parent;
        public final List<Part> children = new ArrayList<>();
        public String imageResourceId;
        public Texture texture;
        public float[] uv;
        public final int flags;
        public Part(int id, String name, Part parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
            this.flags = computeFlags(name);
            if (parent != null) {
                parent.children.add(this);
            }
        }
        private static int computeFlags(String name) {
            int f = 0;
            if (name == null) return f;
            if (name.equals("butter")) f |= FLAG_BUTTER;
            if (name.equals("ink")) f |= FLAG_INK;
            if (name.contains("armor")) f |= FLAG_ARMOR;
            if (name.contains("custom")) f |= FLAG_CUSTOM;
            if (name.equals("ground_swatch")) f |= FLAG_GROUND_SWATCH;
            if (name.equals("ground_swatch_plane")) f |= FLAG_GROUND_SWATCH_PLANE;
            if (name.contains("arm_outer_upper_bone")) f |= FLAG_ARM_UPPER_BONE;
            return f;
        }
    }
    static final class BakedFrame {
        final int count;
        final float[] corners;      
        final float[] colors;       
        final int[] partIds;        
        BakedFrame(int count, float[] corners, float[] colors, int[] partIds) {
            this.count = count;
            this.corners = corners;
            this.colors = colors;
            this.partIds = partIds;
        }
    }
    private static final String WHOLE_KEY = "whole";
    private final ResourceIndex resourceIndex;
    private final PamData animation;
    final float frameRate;
    final float canvasWidth;
    final float canvasHeight;
    final BakedFrame[] frames;
    private final LinkedHashMap<String, int[]> clipRanges;
    private final Map<String, Rectangle> boundsByClip = new HashMap<>();

    public final Part[] parts;
    private List<Part> _tempParts;
    public final Part rootPart;
    BakedAnimation(PamData animation, ResourceIndex resourceIndex) {
        this._tempParts = new ArrayList<>();
        this.resourceIndex = resourceIndex;
        this.animation = animation;
        this.canvasWidth = animation.size != null && animation.size.length >= 2 ? animation.size[0] : 0f;
        this.canvasHeight = animation.size != null && animation.size.length >= 2 ? animation.size[1] : 0f;
        PamData.Sprite main = animation.mainSprite;
        String rootName = main != null && main.name != null && !main.name.isEmpty() ? main.name : "root";
        this.rootPart = getOrCreatePart(rootName, null);
        float fps = main != null && main.frameRate > 0 ? main.frameRate : animation.frameRate;
        this.frameRate = Math.max(1f, fps);
        this.clipRanges = buildClipRanges(main);
        this.frames = bake(main);
        this.parts = this._tempParts.toArray(new Part[0]);
        this._tempParts = null;
    }
    int[] range(String clip) {
        if (clip == null || clip.isEmpty()) {
            return new int[] { 0, Math.max(0, frames.length - 1) };
        }
        int[] r = clipRanges.get(clip);
        if (r == null) {
            throw new IllegalArgumentException(
                "Unknown clip '" + clip + "'; available clips: " + clipRanges.keySet());
        }
        return r;
    }
    float duration(String clip) {
        if (clip == null || clip.isEmpty()) {
            return (Math.max(0, frames.length - 1)  + 1) / frameRate;
        }
        int[] r = clipRanges.get(clip);
        if (r == null) {
            throw new IllegalArgumentException(
                "Unknown clip '" + clip + "'; available clips: " + clipRanges.keySet());
        }
        return (r[1] - r[0] + 1) / frameRate;
    }
    List<String> clips() {
        return new ArrayList<>(clipRanges.keySet());
    }
    Rectangle bounds(String clip) {
        String key = clip == null || clip.isEmpty() ? WHOLE_KEY : clip;
        Rectangle cached = boundsByClip.get(key);
        if (cached == null) {
            int[] r = range(clip);
            cached = computeBounds(r[0], r[1]);
            boundsByClip.put(key, cached);
        }
        return new Rectangle(cached);
    }
    private Rectangle computeBounds(int start, int end) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int f = start; f <= end && f < frames.length; f++) {
            BakedFrame frame = frames[f];
            for (int i = 0; i < frame.count * 8; i += 2) {
                float x = frame.corners[i], y = frame.corners[i + 1];
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        float cx = canvasWidth > 0 ? canvasWidth / 2f : 0.5f;
        float cy = canvasHeight > 0 ? canvasHeight / 2f : 0.5f;
        if (minX > maxX || minY > maxY) {
            return new Rectangle(-cx, -cy, Math.max(1f, canvasWidth), Math.max(1f, canvasHeight));
        }
        return new Rectangle(minX - cx, minY - cy, maxX - minX, maxY - minY);
    }
    private BakedFrame[] bake(PamData.Sprite main) {
        if (main == null || main.frames.isEmpty()) {
            return new BakedFrame[0];
        }
        Runtime runtime = new Runtime(main);
        int n = main.frames.size();
        BakedFrame[] out = new BakedFrame[n];
        for (int f = 0; f < n; f++) {
            applyFrame(runtime, main.frames.get(f));
            advanceChildren(runtime);
            out[f] = flatten(runtime);
        }
        return out;
    }
    private Part getOrCreatePart(String name, Part parent) {
        if (name == null) name = "";
        for (int i = 0; i < _tempParts.size(); i++) {
            Part p = _tempParts.get(i);
            if (p.parent == parent && p.name.equals(name)) {
                return p;
            }
        }
        int id = _tempParts.size();
        Part p = new Part(id, name, parent);
        _tempParts.add(p);
        return p;
    }
    private BakedFrame flatten(Runtime main) {
        List<Quad> quads = new ArrayList<>();
        collect(main, new Matrix2D(), Color.WHITE, this.rootPart, quads);
        int count = quads.size();
        float[] corners = new float[count * 8];
        float[] colors = new float[count];
        int[] partIds = new int[count];
        for (int i = 0; i < count; i++) {
            Quad q = quads.get(i);
            System.arraycopy(q.corners, 0, corners, i * 8, 8);
            colors[i] = q.color;
            partIds[i] = q.part.id;
        }
        return new BakedFrame(count, corners, colors, partIds);
    }
    private void collect(Runtime runtime, Matrix2D parent, Color inherited, Part governor, List<Quad> quads) {
        for (Attachment a : runtime.attachments.values()) {
            Matrix2D world = Matrix2D.multiply(parent, a.transform, new Matrix2D());
            Color color = a.color != null ? a.color : inherited;
            Part part = getOrCreatePart(a.name, governor);
            if (a.child != null) {
                collect(a.child, world, color, part, quads);
            } else if (a.image != null) {
                emit(world, a.image, color, part, quads);
            }
        }
    }
    private void emit(Matrix2D world, PamData.Image image, Color color, Part part, List<Quad> quads) {
        if (part.imageResourceId == null) {
            part.imageResourceId = image.resourceId;
        }
        Matrix2D m = Matrix2D.multiply(world, Matrix2D.fromPacked(image.transform), new Matrix2D());
        float w = 1f;
        float h = 1f;
        if (image.size != null && image.size.length >= 2) {
            w = image.size[0];
            h = image.size[1];
        } else if (resourceIndex != null) {
            ResourceIndex.ImageEntry entry = resourceIndex.image(image.resourceId);
            if (entry != null) {
                w = entry.aw;
                h = entry.ah;
            }
        }
        Quad q = new Quad();
        q.corners[0] = m.worldX(0, 0); q.corners[1] = m.worldY(0, 0);
        q.corners[2] = m.worldX(0, h); q.corners[3] = m.worldY(0, h);
        q.corners[4] = m.worldX(w, h); q.corners[5] = m.worldY(w, h);
        q.corners[6] = m.worldX(w, 0); q.corners[7] = m.worldY(w, 0);
        q.color = color.toFloatBits();
        q.part = part;
        quads.add(q);
    }
    void initializeTextures(TextureBank textures) {
        for (int i = 0; i < parts.length; i++) {
            Part part = parts[i];
            if (part.imageResourceId != null && part.texture == null) {
                TextureRegion region = textures.region(part.imageResourceId);
                if (region != null) {
                    part.texture = region.getTexture();
                    part.uv = new float[] { region.getU(), region.getV(), region.getU2(), region.getV2() };
                }
            }
        }
    }
    private void applyFrame(Runtime runtime, PamData.Frame frame) {
        for (PamData.Remove remove : frame.removes) {
            runtime.attachments.remove(remove.index);
        }
        for (PamData.Add add : frame.appends) {
            runtime.attachments.put(add.index, createAttachment(add));
        }
        for (PamData.Move move : frame.changes) {
            Attachment a = runtime.attachments.get(move.index);
            if (a == null) {
                continue;
            }
            a.transform.set(Matrix2D.fromPacked(move.transform));
            if (move.color != null) {
                a.color = new Color(move.color[0], move.color[1], move.color[2], move.color[3]);
            }
            if (a.child != null && move.animFrameNum != 0) {
                a.child.frameIndex = clamp(a.child, move.animFrameNum);
                a.child.stopped = false;
            }
        }
    }
    private Attachment createAttachment(PamData.Add add) {
        Attachment a = new Attachment();
        a.transform = new Matrix2D();
        a.timescale = add.timescale <= 0f ? 1f : add.timescale;
        String partName = add.name;
        if (add.sprite) {
            if (add.resource >= 0 && add.resource < animation.sprites.size()) {
                PamData.Sprite sprite = animation.sprites.get(add.resource);
                a.child = new Runtime(sprite);
                primeRuntime(a.child);
                if (partName == null) {
                    partName = sprite.name;
                }
            }
        } else if (add.resource >= 0 && add.resource < animation.images.size()) {
            a.image = animation.images.get(add.resource);
            if (partName == null) {
                partName = a.image.name;
            }
        }
        a.name = partName;
        return a;
    }
    private void primeRuntime(Runtime runtime) {
        if (runtime.sprite == null || runtime.sprite.frames.isEmpty()) {
            return;
        }
        applyFrame(runtime, runtime.sprite.frames.get(clamp(runtime, runtime.frameIndex)));
    }
    private void advanceChildren(Runtime runtime) {
        for (Attachment a : runtime.attachments.values()) {
            if (a.child == null) {
                continue;
            }
            a.childAccumulator += Math.max(0f, a.timescale);
            int guard = 0;
            while (a.childAccumulator >= 1f && guard++ < 1024) {
                a.childAccumulator -= 1f;
                stepChild(a.child);
            }
        }
    }
    private void stepChild(Runtime runtime) {
        if (runtime.sprite == null || runtime.sprite.frames.isEmpty() || runtime.stopped) {
            return;
        }
        PamData.Frame frame = runtime.sprite.frames.get(clamp(runtime, runtime.frameIndex));
        applyFrame(runtime, frame);
        advanceChildren(runtime);
        if (frame.stop) {
            runtime.stopped = true;
        } else {
            runtime.frameIndex = next(runtime, runtime.frameIndex);
        }
    }
    private static int clamp(Runtime runtime, int frame) {
        int min = 0;
        int max = runtime.end();
        return (frame < min || frame > max) ? min : frame;
    }
    private static int next(Runtime runtime, int frame) {
        int max = runtime.end();
        int n = frame + 1;
        return n > max ? 0 : n;
    }
    private static LinkedHashMap<String, int[]> buildClipRanges(PamData.Sprite sprite) {
        LinkedHashMap<String, int[]> ranges = new LinkedHashMap<>();
        if (sprite == null) {
            return ranges;
        }
        String active = null;
        int activeStart = 0;
        for (int i = 0; i < sprite.frames.size(); i++) {
            String clip = sprite.frames.get(i).clip;
            if (clip != null && !clip.isEmpty()) {
                if (active != null) {
                    ranges.putIfAbsent(active, new int[] { activeStart, i - 1 });
                }
                active = clip;
                activeStart = i;
            }
        }
        if (active != null) {
            ranges.putIfAbsent(active, new int[] { activeStart, sprite.frames.size() - 1 });
        }
        return ranges;
    }
    private static final class Quad {
        final float[] corners = new float[8];
        float color;
        Part part;
    }
    private static final class Runtime {
        final PamData.Sprite sprite;
        int frameIndex;
        boolean stopped;
        final TreeMap<Integer, Attachment> attachments = new TreeMap<>();
        Runtime(PamData.Sprite sprite) {
            this.sprite = sprite;
        }
        int end() {
            return sprite == null || sprite.frames.isEmpty() ? 0 : sprite.frames.size() - 1;
        }
    }
    private static final class Attachment {
        String name;
        PamData.Image image;
        Runtime child;
        Matrix2D transform;
        Color color;
        float timescale = 1f;
        float childAccumulator;
    }
}
