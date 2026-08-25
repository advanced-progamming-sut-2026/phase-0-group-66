package pvz.libpvz;

public final class Matrix2D {
    public float a, b, c, d, tx, ty;
    public Matrix2D() {
        this(1f, 0f, 0f, 1f, 0f, 0f);
    }
    public Matrix2D(float a, float b, float c, float d, float tx, float ty) {
        set(a, b, c, d, tx, ty);
    }
    public Matrix2D set(float a, float b, float c, float d, float tx, float ty) {
        this.a = a; this.b = b; this.c = c; this.d = d; this.tx = tx; this.ty = ty;
        return this;
    }
    public Matrix2D set(Matrix2D o) {
        return set(o.a, o.b, o.c, o.d, o.tx, o.ty);
    }
    public static Matrix2D multiply(Matrix2D left, Matrix2D right, Matrix2D out) {
        float a = left.a * right.a + left.c * right.b;
        float b = left.b * right.a + left.d * right.b;
        float c = left.a * right.c + left.c * right.d;
        float d = left.b * right.c + left.d * right.d;
        float tx = left.a * right.tx + left.c * right.ty + left.tx;
        float ty = left.b * right.tx + left.d * right.ty + left.ty;
        return out.set(a, b, c, d, tx, ty);
    }
    public float worldX(float x, float y) {
        return a * x + c * y + tx;
    }
    public float worldY(float x, float y) {
        return b * x + d * y + ty;
    }
    public static Matrix2D fromPacked(float[] p) {
        if (p == null || p.length == 0) {
            return new Matrix2D();
        }
        if (p.length >= 6) {
            return new Matrix2D(p[0], p[1], p[2], p[3], p[4], p[5]);
        }
        if (p.length == 3) {
            float cos = (float) Math.cos(p[0]);
            float sin = (float) Math.sin(p[0]);
            return new Matrix2D(cos, sin, -sin, cos, p[1], p[2]);
        }
        if (p.length == 2) {
            return new Matrix2D(1f, 0f, 0f, 1f, p[0], p[1]);
        }
        return new Matrix2D();
    }
}