package org.jrgss.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.GlyphSlot;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Library;
import com.badlogic.gdx.graphics.glutils.FloatFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.sun.jna.Pointer;
import org.jrgss.shaders.FontShaders;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FontRenderer {
    public static final int MAX_TRIANGLES = 512;
    public static final int MAX_SIZE = 6144;
    private static final List<FontRenderer.SampleParameter> SAMPLE_PARAMETERS = ImmutableList.of(
        new FontRenderer.SampleParameter(0.4166F, -0.4166F, 1.0F, 0.0F, 0.0F),
        new FontRenderer.SampleParameter(0.25F, 0.0833F, 256.0F, 0.0F, 0.0F),
        new FontRenderer.SampleParameter(0.0833F, -0.0833F, 0.0F, 1.0F, 0.0F),
        new FontRenderer.SampleParameter(-0.0833F, 0.4166F, 0.0F, 256.0F, 0.0F),
        new FontRenderer.SampleParameter(-0.25F, -0.25F, 0.0F, 0.0F, 1.0F),
        new FontRenderer.SampleParameter(-0.4166F, 0.25F, 0.0F, 0.0F, 256.0F)
    );
    private static final byte TAG_ONCURVE = 1;
    private static final byte TAG_BEZIER3 = 2;
    private static Field addressField;

    static {
        init();
    }

    private final Library library;
    private final Face face;
    private final FloatFrameBuffer buffer;
    private final FloatBuffer vertices;
    private final Mesh lineMesh;
    private final Mesh curveMesh;

    public FontRenderer(FileHandle fontFile) {
        int fileSize = (int) fontFile.length();
        this.library = FreeType.initFreeType();
        if (this.library == null) {
            throw new GdxRuntimeException("Couldn't initialize FreeType");
        } else {
            InputStream input = fontFile.read();

            ByteBuffer buffer;
            try {
                if (fileSize == 0) {
                    byte[] data = StreamUtils.copyStreamToByteArray(input, 16384);
                    buffer = BufferUtils.newUnsafeByteBuffer(data.length);
                    BufferUtils.copy(data, 0, buffer, data.length);
                } else {
                    buffer = BufferUtils.newUnsafeByteBuffer(fileSize);
                    StreamUtils.copyStream(input, buffer);
                }
            } catch (IOException var9) {
                throw new GdxRuntimeException(var9);
            } finally {
                StreamUtils.closeQuietly(input);
            }

            this.face = this.library.newMemoryFace(buffer, 0);
            if (this.face == null) {
                throw new GdxRuntimeException("Couldn't create face for font: " + fontFile);
            } else {
                this.buffer = new FloatFrameBuffer(128, 128, false);
                this.buffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                this.vertices = FloatBuffer.allocate(6144);
                this.lineMesh = new Mesh(false, 1536, 0, new VertexAttribute(1, 2, "a_position"));
                this.curveMesh = new Mesh(false, 1536, 0, new VertexAttribute(1, 2, "a_position"), new VertexAttribute(16, 2, "a_texCoord0"));
            }
        }
    }

    private static float scale(float size) {
        return 0.038888F * (size / 12.0F);
    }

    private static FTGlyphSlot getFTGlyphSlot(GlyphSlot glyphSlot) {
        try {
            long addr = addressField.getLong(glyphSlot);
            return new FTGlyphSlot(new Pointer(addr));
        } catch (Exception var3) {
            throw new RuntimeException("Failed to convert GlyphSlot into an FTGlyphSlot", var3);
        }
    }

    public static void init() {
        try {
            addressField = GlyphSlot.class.getSuperclass().getDeclaredField("address");
            addressField.setAccessible(true);
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

    public Texture renderCharacter(char chara, float size) {
        FontRenderer.GlyphMetrics glyphMetrics = new FontRenderer.GlyphMetrics(chara, size);
        Matrix4 projectionMatrix = this.buildProjection(glyphMetrics);
        Gdx.gl20.glEnable(3042);
        Gdx.gl20.glBlendEquationSeparate(32774, 32774);
        Gdx.gl20.glBlendFuncSeparate(1, 1, 1, 0);
        this.buffer.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Gdx.gl.glClear(16384);
        this.vertices(chara, glyphMetrics.getYMin());
        this.lineMesh.setVertices(this.vertices.array(), 0, this.vertices.position());
        ShaderProgram program = FontShaders.getLineShader();
        program.begin();

        for (FontRenderer.SampleParameter param : SAMPLE_PARAMETERS) {
            program.setUniformMatrix(FontShaders.PROJECTION_MATRIX_UNIFORM, projectionMatrix);
            program.setUniformMatrix(FontShaders.MODELVIEW_MATRIX_UNIFORM, this.buildModelview(size, 1.0F, param));
            program.setUniform3fv(FontShaders.ACCUMULATION_UNIFORM, param.accumulation, 0, 3);
            this.lineMesh.render(program, 4);
        }

        program.end();
        this.verticesCurve(chara, glyphMetrics.getYMin());
        this.curveMesh.setVertices(this.vertices.array(), 0, this.vertices.position());
        program = FontShaders.getCurveShader();
        program.begin();

        for (FontRenderer.SampleParameter param : SAMPLE_PARAMETERS) {
            program.setUniformMatrix(FontShaders.PROJECTION_MATRIX_UNIFORM, projectionMatrix);
            program.setUniformMatrix(FontShaders.MODELVIEW_MATRIX_UNIFORM, this.buildModelview(size, 1.0F, param));
            program.setUniform3fv(FontShaders.ACCUMULATION_UNIFORM, param.accumulation, 0, 3);
            this.curveMesh.render(program, 4);
        }

        program.end();
        this.buffer.end();
        return this.buffer.getColorBufferTexture();
    }

    private void vertices(char chara, double ymin) {
        this.loadCharacter(chara);
        FTGlyphSlot glyphSlot = getFTGlyphSlot(this.face.getGlyph());
        FontRenderer.ContourIterator iterator = new FontRenderer.ContourIterator(glyphSlot);
        ((Buffer) this.vertices).limit(this.vertices.capacity()).rewind();

        while (iterator.hasNext()) {
            FontRenderer.CurveIterator contour = iterator.next();
            FTGlyphSlot.FTVector start = contour.start();
            FTGlyphSlot.FTVector curr = contour.start();

            while (contour.hasNext()) {
                FontRenderer.Curve curve = contour.next();
                curr = this.drawLines(curr, curve, ymin);
            }

            this.addFontVertex(curr, start, ymin);
        }
    }

    private void verticesCurve(char chara, double ymin) {
        this.loadCharacter(chara);
        FTGlyphSlot glyphSlot = getFTGlyphSlot(this.face.getGlyph());
        FontRenderer.ContourIterator iterator = new FontRenderer.ContourIterator(glyphSlot);
        ((Buffer) this.vertices).limit(this.vertices.capacity()).rewind();

        while (iterator.hasNext()) {
            FontRenderer.CurveIterator contour = iterator.next();
            FTGlyphSlot.FTVector curr = contour.start();

            while (contour.hasNext()) {
                FontRenderer.Curve curve = contour.next();
                curr = this.drawCurve(curr, curve, ymin);
            }
        }
    }

    private FTGlyphSlot.FTVector drawLines(FTGlyphSlot.FTVector last, FontRenderer.Curve curve, double ymin) {
        switch (curve.type()) {
            case Line:
                this.addFontVertex(last, curve.pt(0), ymin);
                return curve.pt(0);
            case Bezier2:
                this.addFontVertex(last, curve.pt(1), ymin);
                return curve.pt(1);
            case Bezier3:
                this.addFontVertex(last, curve.pt(2), ymin);
                return curve.pt(2);
            default:
                throw new IllegalStateException("Unknown curve type");
        }
    }

    private void addFontVertex(FTGlyphSlot.FTVector pt1, FTGlyphSlot.FTVector pt2, double ymin) {
        this.vertices.put(100.0F).put(0.0F);
        this.vertices.put((float) pt1.x).put((float) pt1.y + (float) ymin);
        this.vertices.put((float) pt2.x).put((float) pt2.y + (float) ymin);
    }

    private FTGlyphSlot.FTVector drawCurve(FTGlyphSlot.FTVector last, FontRenderer.Curve curve, double ymin) {
        switch (curve.type()) {
            case Line:
                return curve.pt(0);
            case Bezier2:
                this.addCurveVertex(last, curve.pt(0), curve.pt(1), ymin);
                return curve.pt(1);
            case Bezier3:
                return curve.pt(2);
            default:
                throw new IllegalStateException("Unknown curve type");
        }
    }

    private void addCurveVertex(FTGlyphSlot.FTVector pt1, FTGlyphSlot.FTVector pt2, FTGlyphSlot.FTVector pt3, double ymin) {
        this.vertices.put((float) pt1.x).put((float) pt1.y + (float) ymin).put(0.0F).put(0.0F);
        this.vertices.put((float) pt2.x).put((float) pt2.y + (float) ymin).put(0.5F).put(0.0F);
        this.vertices.put((float) pt3.x).put((float) pt3.y + (float) ymin).put(1.0F).put(1.0F);
    }

    private void loadCharacter(char character) {
        this.face.setCharSize(0, 24, 0, 256);
        if (!this.face.loadChar(character, FreeType.FT_LOAD_TARGET_NORMAL)) {
            System.err.println("Failed to load character: " + character);
        }
    }

    private Matrix4 buildProjection(FontRenderer.GlyphMetrics metrics) {
        float width = this.buffer.getWidth();
        float height = this.buffer.getHeight();
        return new Matrix4()
            .scale(2.0F / width, 2.0F / height, 1.0F)
            .translate(-1.0F + (1.0F / metrics.width + 2.0F), -1.0F + 3.0F / (metrics.height + 6.0F), 0.0F);
    }

    private Matrix4 buildModelview(float fontSize, float hidpi, FontRenderer.SampleParameter param) {
        float scale = scale(fontSize);
        return new Matrix4().scale(scale, scale, 1.0F).translate(param.x * scale * (16.0F / hidpi), param.y * scale * (16.0F / hidpi), 0.0F);
    }

    private enum CurveType {
        Line,
        Bezier2,
        Bezier3
    }

    private static class ContourIterator extends AbstractIterator<FontRenderer.CurveIterator> {
        private final Pointer points;
        private final Pointer tags;
        private Pointer endIdx;
        private Pointer lastIdx;
        private int start = 0;
        private int num;
        private int current = 0;

        public ContourIterator(FTGlyphSlot glyphSlot) {
            FTGlyphSlot.FTOutline outline = glyphSlot.outline;
            this.points = outline.points;
            this.tags = outline.tags;
            this.endIdx = outline.contours;
            this.lastIdx = outline.contours.share(outline.numContours * 2);
            this.num = outline.numContours;
        }

        @ConstructorProperties({"points", "tags"})
        public ContourIterator(Pointer points, Pointer tags) {
            this.points = points;
            this.tags = tags;
        }

        protected FontRenderer.CurveIterator computeNext() {
            if (this.current == this.num) {
                return this.endOfData();
            } else {
                short contourEnd = this.endIdx.getShort(0L);
                FontRenderer.CurveIterator result = new FontRenderer.CurveIterator(
                    this.points.share((long) this.start * FTGlyphSlot.FTVector.SIZE), this.tags.share(this.start), contourEnd - this.start + 1, 0
                );
                this.start = contourEnd + 1;
                this.endIdx = this.endIdx.share(2L);
                this.current++;
                return result;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof FontRenderer.ContourIterator)) {
                return false;
            } else {
                FontRenderer.ContourIterator other = (FontRenderer.ContourIterator) o;
                if (!other.canEqual(this)) {
                    return false;
                } else if (!super.equals(o)) {
                    return false;
                } else {
                    Object this$points = this.points;
                    Object other$points = other.points;
                    if (Objects.equals(this$points, other$points)) {
                        Object this$tags = this.tags;
                        Object other$tags = other.tags;
                        if (Objects.equals(this$tags, other$tags)) {
                            Object this$endIdx = this.endIdx;
                            Object other$endIdx = other.endIdx;
                            if (Objects.equals(this$endIdx, other$endIdx)) {
                                Object this$lastIdx = this.lastIdx;
                                Object other$lastIdx = other.lastIdx;
                                if (Objects.equals(this$lastIdx, other$lastIdx)) {
                                    if (this.start != other.start) {
                                        return false;
                                    } else {
                                        return this.num == other.num && this.current == other.current;
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof FontRenderer.ContourIterator;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + super.hashCode();
            Object $points = this.points;
            result = result * 59 + ($points == null ? 43 : $points.hashCode());
            Object $tags = this.tags;
            result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
            Object $endIdx = this.endIdx;
            result = result * 59 + ($endIdx == null ? 43 : $endIdx.hashCode());
            Object $lastIdx = this.lastIdx;
            result = result * 59 + ($lastIdx == null ? 43 : $lastIdx.hashCode());
            result = result * 59 + this.start;
            result = result * 59 + this.num;
            return result * 59 + this.current;
        }
    }

    private static class Curve {
        private final FTGlyphSlot.FTVector[] vectors;

        public Curve(FTGlyphSlot.FTVector... vectors) {
            if (vectors.length <= 3 && vectors.length != 0) {
                this.vectors = vectors;
            } else {
                throw new IllegalArgumentException("Curve must be 1, 2, or 3 vectors long");
            }
        }

        public FontRenderer.CurveType type() {
            switch (this.vectors.length) {
                case 1:
                    return FontRenderer.CurveType.Line;
                case 2:
                    return FontRenderer.CurveType.Bezier2;
                case 3:
                    return FontRenderer.CurveType.Bezier3;
                default:
                    throw new IllegalStateException("Illegal Vector Length!");
            }
        }

        public FTGlyphSlot.FTVector pt(int i) {
            return this.vectors[i];
        }

        @Override
        public String toString() {
            return this.type() + "{ " + Arrays.toString(this.vectors) + " }";
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof FontRenderer.Curve)) {
                return false;
            } else {
                FontRenderer.Curve other = (FontRenderer.Curve) o;
                return other.canEqual(this) && Arrays.deepEquals(this.vectors, other.vectors);
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof FontRenderer.Curve;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            return result * 59 + Arrays.deepHashCode(this.vectors);
        }
    }

    private static class CurveIterator extends AbstractIterator<FontRenderer.Curve> {
        final Pointer startPoint;
        final Pointer startTag;
        final int length;
        int idx;

        @ConstructorProperties({"startPoint", "startTag", "length", "idx"})
        public CurveIterator(Pointer startPoint, Pointer startTag, int length, int idx) {
            this.startPoint = startPoint;
            this.startTag = startTag;
            this.length = length;
            this.idx = idx;
        }

        public FTGlyphSlot.FTVector start() {
            FTGlyphSlot.FTVector start = new FTGlyphSlot.FTVector(this.startPoint);
            if (this.startOnCurve()) {
                return start;
            } else {
                FTGlyphSlot.FTVector next = this.pt(1);
                return new FTGlyphSlot.FTVector((start.x + next.x) / 2L, (start.y + next.y) / 2L);
            }
        }

        public FTGlyphSlot.FTVector pt(int i) {
            return this.idx + i < this.length
                ? new FTGlyphSlot.FTVector(this.startPoint.share((long) (this.idx + i) * FTGlyphSlot.FTVector.SIZE))
                : new FTGlyphSlot.FTVector(this.startPoint.share((long) (this.idx + i - this.length) * FTGlyphSlot.FTVector.SIZE));
        }

        public byte tag(int i) {
            if (this.idx + i < this.length) {
                this.startPoint.share(this.idx + i);
            }

            return this.startTag.getByte(0L);
        }

        private boolean startOnCurve() {
            return (this.startTag.getByte(0L) & FontRenderer.TAG_ONCURVE) == FontRenderer.TAG_ONCURVE;
        }

        protected FontRenderer.Curve computeNext() {
            if (this.idx >= this.length) {
                return this.endOfData();
            } else {
                byte tag1 = this.tag(1);
                if ((tag1 & FontRenderer.TAG_ONCURVE) == FontRenderer.TAG_ONCURVE) {
                    FontRenderer.Curve ret = new FontRenderer.Curve(this.pt(1));
                    this.idx++;
                    return ret;
                } else if ((tag1 & FontRenderer.TAG_BEZIER3) == FontRenderer.TAG_BEZIER3) {
                    FontRenderer.Curve ret = new FontRenderer.Curve(this.pt(1), this.pt(2), this.pt(3));
                    this.idx += 3;
                    return ret;
                } else if ((this.tag(2) & FontRenderer.TAG_ONCURVE) == FontRenderer.TAG_ONCURVE) {
                    FontRenderer.Curve ret = new FontRenderer.Curve(this.pt(1), this.pt(2));
                    this.idx += 2;
                    return ret;
                } else {
                    FTGlyphSlot.FTVector pt1 = this.pt(1);
                    FTGlyphSlot.FTVector pt2 = this.pt(2);
                    FontRenderer.Curve ret = new FontRenderer.Curve(this.pt(1), new FTGlyphSlot.FTVector((pt1.x + pt2.x) / 2L, (pt1.y + pt2.y) / 2L));
                    this.idx++;
                    return ret;
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof FontRenderer.CurveIterator)) {
                return false;
            } else {
                FontRenderer.CurveIterator other = (FontRenderer.CurveIterator) o;
                if (!other.canEqual(this)) {
                    return false;
                } else if (!super.equals(o)) {
                    return false;
                } else {
                    Object this$startPoint = this.startPoint;
                    Object other$startPoint = other.startPoint;
                    if (Objects.equals(this$startPoint, other$startPoint)) {
                        Object this$startTag = this.startTag;
                        Object other$startTag = other.startTag;
                        if (Objects.equals(this$startTag, other$startTag)) {
                            return this.length == other.length && this.idx == other.idx;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof FontRenderer.CurveIterator;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + super.hashCode();
            Object $startPoint = this.startPoint;
            result = result * 59 + ($startPoint == null ? 43 : $startPoint.hashCode());
            Object $startTag = this.startTag;
            result = result * 59 + ($startTag == null ? 43 : $startTag.hashCode());
            result = result * 59 + this.length;
            return result * 59 + this.idx;
        }
    }

    private static class SampleParameter {
        private static final float ACCUMULATION_SCALAR = 65535.0F;
        private final float x;
        private final float y;
        private final float[] accumulation;

        public SampleParameter(float x, float y, float... accumulation) {
            this.x = x;
            this.y = y;
            this.accumulation = accumulation;

            for (int i = 0; i < accumulation.length; i++) {
                accumulation[i] /= 65535.0F;
            }
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public float[] getAccumulation() {
            return this.accumulation;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof FontRenderer.SampleParameter)) {
                return false;
            } else {
                FontRenderer.SampleParameter other = (FontRenderer.SampleParameter) o;
                if (!other.canEqual(this)) {
                    return false;
                } else if (Float.compare(this.getX(), other.getX()) != 0) {
                    return false;
                } else {
                    return Float.compare(this.getY(), other.getY()) == 0 && Arrays.equals(this.getAccumulation(), other.getAccumulation());
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof FontRenderer.SampleParameter;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + Float.floatToIntBits(this.getX());
            result = result * 59 + Float.floatToIntBits(this.getY());
            return result * 59 + Arrays.hashCode(this.getAccumulation());
        }

        @Override
        public String toString() {
            return "FontRenderer.SampleParameter(x=" + this.getX() + ", y=" + this.getY() + ", accumulation=" + Arrays.toString(this.getAccumulation()) + ")";
        }
    }

    private class GlyphMetrics {
        private final float yMin;
        private final float xBearing;
        private final float yBearing;
        private final float advance;
        private final float width;
        private final float height;
        private final float size;

        public GlyphMetrics(char character, float size) {
            FontRenderer.this.loadCharacter(character);
            FreeType.GlyphMetrics metrics = FontRenderer.this.face.getGlyph().getMetrics();
            float fontScale = FontRenderer.scale(size);
            this.yBearing = metrics.getHoriBearingY() * fontScale;
            this.height = metrics.getHeight() * fontScale;
            this.yMin = this.yBearing - this.height;
            this.xBearing = metrics.getHoriBearingX() * fontScale;
            this.advance = metrics.getHoriAdvance() * fontScale;
            this.width = metrics.getWidth() * fontScale;
            this.size = size;
        }

        public float getYMin() {
            return this.yMin;
        }

        public float getXBearing() {
            return this.xBearing;
        }

        public float getYBearing() {
            return this.yBearing;
        }

        public float getAdvance() {
            return this.advance;
        }

        public float getWidth() {
            return this.width;
        }

        public float getHeight() {
            return this.height;
        }

        public float getSize() {
            return this.size;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof FontRenderer.GlyphMetrics)) {
                return false;
            } else {
                FontRenderer.GlyphMetrics other = (FontRenderer.GlyphMetrics) o;
                if (!other.canEqual(this)) {
                    return false;
                } else if (Float.compare(this.getYMin(), other.getYMin()) != 0) {
                    return false;
                } else if (Float.compare(this.getXBearing(), other.getXBearing()) != 0) {
                    return false;
                } else if (Float.compare(this.getYBearing(), other.getYBearing()) != 0) {
                    return false;
                } else if (Float.compare(this.getAdvance(), other.getAdvance()) != 0) {
                    return false;
                } else if (Float.compare(this.getWidth(), other.getWidth()) != 0) {
                    return false;
                } else {
                    return Float.compare(this.getHeight(), other.getHeight()) == 0 && Float.compare(this.getSize(), other.getSize()) == 0;
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof FontRenderer.GlyphMetrics;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + Float.floatToIntBits(this.getYMin());
            result = result * 59 + Float.floatToIntBits(this.getXBearing());
            result = result * 59 + Float.floatToIntBits(this.getYBearing());
            result = result * 59 + Float.floatToIntBits(this.getAdvance());
            result = result * 59 + Float.floatToIntBits(this.getWidth());
            result = result * 59 + Float.floatToIntBits(this.getHeight());
            return result * 59 + Float.floatToIntBits(this.getSize());
        }

        @Override
        public String toString() {
            return "FontRenderer.GlyphMetrics(yMin="
                + this.getYMin()
                + ", xBearing="
                + this.getXBearing()
                + ", yBearing="
                + this.getYBearing()
                + ", advance="
                + this.getAdvance()
                + ", width="
                + this.getWidth()
                + ", height="
                + this.getHeight()
                + ", size="
                + this.getSize()
                + ")";
        }
    }
}
