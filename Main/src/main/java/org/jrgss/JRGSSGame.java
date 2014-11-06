package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.jrgss.api.*;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.*;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.ThreadContext;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by mcanterb on 6/26/14.
 */
public class JRGSSGame implements JRGSSApplicationListener {
    ScriptingContainer scriptingContainer;
    final String[] BUILTINS = new String[] {
            "Audio", "Bitmap", "Color", "Font", "Graphics",
            "Input", "Plane", "Rect", "RGSSError", "Sprite",
            "Tilemap", "Tone", "Viewport", "Window",
            "Win32API"
    };
    public static final Queue<FutureTask<?>> glRunnables = new ConcurrentLinkedQueue<>();

    static JRGSSMain mainBlock;
    public static String JRGSS_DIR = "/Users/matt/Downloads/JRGSS/Main";
    SpriteBatch batch;
    public static OrthographicCamera camera;
    FPSLogger fpsLogger;
    static Thread glThread;
    static ConfigReader ini;

    public static boolean isRenderThread() {
        return Thread.currentThread() == glThread;
    }

    public JRGSSGame(String gameDirectory, String rtpDirectory, ConfigReader ini) {
        super();
        JRGSSGame.ini = ini;
        RGSSVersion rgss = ini.getRGSSVersion();
        if(rgss != RGSSVersion.VXAce) {
            int result = JOptionPane.showConfirmDialog(null, "This game uses an unsupported version of RGSS. This game is for "+
            rgss+" There may be some incompatibilities!",
            "Unsupported RGSS Version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if(result == JOptionPane.CANCEL_OPTION) {
                System.exit(0);
            }
        }
        FileUtil.setGameDirectory(gameDirectory);
        FileUtil.setRTPDirectory(rtpDirectory);
        File encryptedArchiveFile = new File(gameDirectory+File.separator+"Game.rgss3a");
        if(encryptedArchiveFile.exists()) {
            try{
                EncryptedArchive archive = new EncryptedArchive(gameDirectory+File.separator+"Game.rgss3a");
                FileUtil.setEncryptedArchive(archive);
            }catch (Exception e) {
                System.err.println("Could not load archive!");
                e.printStackTrace(System.err);
            }
        }


    }

    public void loadScriptData(String path) {
        FileHandle f;
        if(FileUtil.archive != null) {
            f = FileUtil.archive.openFile(path);
        } else {
            path = path.replaceAll("\\\\", File.separator);
            f = new FileHandle(FileUtil.gameDirectory + File.separator + path);
        }
        byte[] bytes = f.readBytes();
        RubyString str = RubyString.newString(Ruby.getGlobalRuntime(), bytes);
        RubyArray arr = (RubyArray)RubyMarshal.load(ThreadContext.newContext(Ruby.getGlobalRuntime()),
                null,
                new RubyObject[]{str},
                null);
        int index = 0;
        scriptingContainer.put("$RGSS_SCRIPTS", arr);
        for(Object o : arr) {
            RubyArray item = (RubyArray)o;
            String name = (String)item.get(1);
            System.out.println(name);
            scriptingContainer.put("$__obj", item);
            String str2 = (String)scriptingContainer.runScriptlet("Zlib::Inflate.inflate($__obj[2]).force_encoding(\"utf-8\")");
            try{
                String script = str2;
                try(FileWriter writer = new FileWriter("/Users/matt/monsters/"+index+" - "+name+".rb")) {
                    writer.write(script);
                }
                scriptingContainer.setScriptFilename(name);
                scriptingContainer.runScriptlet("# encoding: UTF-8\n" + script);
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
            scriptingContainer.put("$__obj", null);
            index++;
        }
    }

    public void loadRPGModule() {
        try{
            String[] files = new File(JRGSS_DIR+File.separator+"rpg").list();
            for(String file : files) {
                InputStream stream = new FileInputStream(new File(JRGSS_DIR+
                        File.separator+"rpg"+File.separator+file));
                scriptingContainer.runScriptlet(stream, file);
                System.out.println("Loaded file "+file);
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to load built in ruby scripts!",e);
        }
    }

    public static void jrgssMain(JRGSSMain rubyBlock) {
        mainBlock = rubyBlock;
    }

    public void loadRGSSModule(String name) {
        scriptingContainer.runScriptlet("java_import org.jrgss.api."+name);
        scriptingContainer.runScriptlet("class "+name+"\ndef _dump level\nself.dump\nend\nend");
    }

    @Override
    public void create() {
        glThread = Thread.currentThread();

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        fpsLogger = new FPSLogger();
        Gdx.graphics.setVSync(true);
        batch = new SpriteBatch();
        batch.enableBlending();
    }

    @Override
    public void resize(int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void render() {
        //fpsLogger.log();
        update();
        //Gdx.app.log("JRGSSGame", "$time is "+scriptingContainer.runScriptlet("$time.delta if $time"));
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(1f,1f,1f,1f);
        //batch.begin();
        Graphics.render(batch);
        //batch.end();
    }

    @Override
    public void pause() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resume() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void dispose() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static void runWithGLContext(final Runnable runnable) {
        if(Thread.currentThread() == glThread) {
            runnable.run();
        } else {
            //FutureTask<?> task = new FutureTask<Object>(runnable, null);
            Gdx.app.postRunnable(runnable);
        }
    }

    public void update() {

        if(Gdx.input.isKeyPressed(Input.Keys.F1)) {
            ArrayList<AbstractRenderable> renderables = new ArrayList<>();
            ArrayList<AbstractRenderable> viewportLessRenderables = new ArrayList<>();
            for(AbstractRenderable renderable : AbstractRenderable.renderQueue.values()) {
                if(renderable.getViewport() == null) {
                    viewportLessRenderables.add(renderable);
                } else {
                    renderables.add(renderable);
                }
            }
            Collections.sort(renderables);
            Collections.sort(viewportLessRenderables, Graphics.alternateComparator);
            Iterator<AbstractRenderable> iter = viewportLessRenderables.iterator();
            for (AbstractRenderable renderable : renderables) {

                AbstractRenderable r;
                while(iter.hasNext() && Graphics.alternateComparator.compare((r = iter.next()), renderable) < 0) {
                    System.out.println(String.format("%s: %s, %d, %d, %d ", r.getClass().getSimpleName(), r.getViewport(), r.getZ(), r.getY(), r.getCreationTime()) );
                    if(renderable instanceof Window && ((Window) renderable).getContents().getPath() != null) {
                        System.out.println(String.format("Window has %s, %d", ((Window) renderable).isVisible(), ((Window) renderable).getOpenness()));
                    }
                }
                System.out.println(String.format("%s: %s, %d, %d, %d ", renderable.getClass().getSimpleName(), renderable.getViewport(), renderable.getZ(), renderable.getY(), renderable.getCreationTime()) );
                if(renderable instanceof Window && ((Window) renderable).getContents().getPath() != null) {
                    System.out.println(String.format("Window has %s, %d", ((Window) renderable).isVisible(), ((Window) renderable).getOpenness()));
                }
            }
            while(iter.hasNext()) {
                AbstractRenderable renderable = iter.next();
                System.out.println(String.format("%s: %s, %d, %d, %d ", renderable.getClass().getSimpleName(), renderable.getViewport(), renderable.getZ(), renderable.getY(), renderable.getCreationTime()));
                if(renderable instanceof Window && ((Window) renderable).getContents().getPath() != null) {
                    System.out.println(String.format("Window has %s, %d", ((Window) renderable).isVisible(), ((Window) renderable).getOpenness()));
                }

            }

        }
    }

    @Override
    public void loadScripts() {
        scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
        scriptingContainer.setCompatVersion(CompatVersion.RUBY1_9);
        scriptingContainer.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        scriptingContainer.setRunRubyInProcess(true);
        scriptingContainer.runScriptlet("$TEST=true");
        scriptingContainer.runScriptlet("require 'java'");
        scriptingContainer.runScriptlet("require 'org/jrgss/api/RGSSBuiltin'");
        for(String module: BUILTINS) {
            loadRGSSModule(module);
        }

        loadRPGModule();
        scriptingContainer.put("$_jrgss_home", FileUtil.gameDirectory);
        loadScriptData(ini.getScripts());
    }

    @Override
    public JRGSSMain getMain() {
        return mainBlock;
    }


    public static interface JRGSSMain {
        public void main();
    }


}
