package pvz.libpvz.pam;

import java.util.List;


/**
 * A raw data container representing a parsed PAM file directly loaded from disk.
 * Contains the unbaked hierarchical sprites, frames, and animation commands.
 */
final class PamData {
    public final int version;
    public final float frameRate;
    public final float[] position; 
    public final float[] size;     
    public final List<Image> images;
    public final List<Sprite> sprites;
    public final Sprite mainSprite;
    public PamData(int version, float frameRate, float[] position, float[] size,
                        List<Image> images, List<Sprite> sprites, Sprite mainSprite) {
        this.version = version;
        this.frameRate = frameRate;
        this.position = position;
        this.size = size;
        this.images = images;
        this.sprites = sprites;
        this.mainSprite = mainSprite;
    }
    public static final class Image {
        public final String name;
        public final String resourceId;   
        public final int[] size;          
        public final float[] transform;   
        public Image(String name, String resourceId, int[] size, float[] transform) {
            this.name = name;
            this.resourceId = resourceId;
            this.size = size;
            this.transform = transform;
        }
    }
    public static final class Sprite {
        public final String name;
        public final String description;
        public final float frameRate;
        public final int[] workArea;      
        public final List<Frame> frames;
        public Sprite(String name, String description, float frameRate, int[] workArea, List<Frame> frames) {
            this.name = name;
            this.description = description;
            this.frameRate = frameRate;
            this.workArea = workArea;
            this.frames = frames;
        }
    }
    
    public static final class Frame {
        public final String clip;        
        public final boolean stop;
        public final List<Command> commands;
        public final List<Remove> removes;
        public final List<Add> appends;
        public final List<Move> changes;
        public Frame(String clip, boolean stop, List<Command> commands,
                     List<Remove> removes, List<Add> appends, List<Move> changes) {
            this.clip = clip;
            this.stop = stop;
            this.commands = commands;
            this.removes = removes;
            this.appends = appends;
            this.changes = changes;
        }
    }
    public static final class Add {
        public final int index;
        public final String name;
        public final int resource;
        public final boolean sprite;
        public final boolean additive;
        public final int preloadFrames;
        public final float timescale;

        public Add(int index, String name, int resource, boolean sprite,
                   boolean additive, int preloadFrames, float timescale) {
            this.index = index;
            this.name = name;
            this.resource = resource;
            this.sprite = sprite;
            this.additive = additive;
            this.preloadFrames = preloadFrames;
            this.timescale = timescale;
        }
    }
    public static final class Move {
        public final int index;
        public final float[] transform;   
        public final float[] color;       
        public final int[] srcRect;       
        public final int animFrameNum;    
        public Move(int index, float[] transform, float[] color, int[] srcRect, int animFrameNum) {
            this.index = index;
            this.transform = transform;
            this.color = color;
            this.srcRect = srcRect;
            this.animFrameNum = animFrameNum;
        }
    }
    public static final class Remove {
        public final int index;
        public Remove(int index) { this.index = index; }
    }
    public static final class Command {
        public final String command;
        public final String parameter;
        public Command(String command, String parameter) { this.command = command; this.parameter = parameter; }
    }
}