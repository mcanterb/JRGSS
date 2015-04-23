package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import org.jrgss.api.*;
import org.jrgss.api.Graphics;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.*;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by mcanterb on 6/26/14.
 */
public class JRGSSGame implements JRGSSApplicationListener {
    ScriptingContainer scriptingContainer;
    final String[] BUILTINS = new String[] {
            "Audio", "Bitmap", "Graphics",
            "Plane", "Rect", "RGSSError", "Sprite",
            "Tilemap", "Tone", "Viewport", "Window",
            "RGSSReset"
    };
    public static final Queue<FutureTask<?>> glRunnables = new ConcurrentLinkedQueue<>();

    static JRGSSMain mainBlock;
    static String JRGSS_DIR;
    SpriteBatch batch;
    public static OrthographicCamera camera;
    FPSLogger fpsLogger;
    static Thread glThread;
    static ConfigReader ini;

    public static boolean isRenderThread() {
        return Thread.currentThread() == glThread;
    }

    public JRGSSGame(String gameDirectory, String rtpDirectory, String jrgssDir, ConfigReader ini) {
        super();
        JRGSSGame.ini = ini; //Pretty shitty, but JRGSSGame should be a singleton
        JRGSSGame.JRGSS_DIR = jrgssDir;
        FileUtil.setLocalDirectory(ini.getTitle());
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

    public void loadScriptsFromDirectory(String path) {
        File[] fileList = new File(path).listFiles();
        File[] orderedList = new File[fileList.length];
        for(File f : fileList) {
            String fileName = f.getName();
            String[] nameParts = fileName.split("@@__@@");
            orderedList[Integer.parseInt(nameParts[0])] = f;
        }

        for(File f : orderedList) {
            try{
                scriptingContainer.runScriptlet(new FileReader(f),f.getName().split("@@__@@")[1]);
            }catch(Exception e) {
                throw new RuntimeException(e);
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
                String script = str2.replaceAll("\r\n","\n");
                
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
        if(SplashScreen.getSplashScreen()!=null) {
            SplashScreen.getSplashScreen().close();
        }
        glThread = Thread.currentThread();

        camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
        camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
        camera.update();
        fpsLogger = new FPSLogger();
        Gdx.graphics.setVSync(true);
        batch = new SpriteBatch();
        batch.enableBlending();
       // Graphics.initialize();

    }

    @Override
    public void resize(int width, int height) {
        camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
        camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
        camera.update();
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

    DebugFrame debugFrame = new DebugFrame();
    boolean buttonTrigger = false;

    public void update() {

        if(!buttonTrigger && Gdx.input.isKeyPressed(Input.Keys.F1)) {

            debugFrame.refresh();
            buttonTrigger = true;
        }
        if(buttonTrigger && !Gdx.input.isKeyPressed(Input.Keys.F1)) {
            buttonTrigger = false;
        }


    }

    static boolean override = false;

    @Override
    public void loadScripts() {
        scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
        scriptingContainer.setCompatVersion(CompatVersion.RUBY1_9);
        scriptingContainer.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        scriptingContainer.setRunRubyInProcess(true);
        //scriptingContainer.runScriptlet("$TEST=true");
        scriptingContainer.runScriptlet("require 'java'");
        scriptingContainer.runScriptlet("require 'org/jrgss/api/RGSSBuiltin'");
        for(String module: BUILTINS) {
            loadRGSSModule(module);
        }

        loadRPGModule();
        scriptingContainer.put("$_jrgss_home", FileUtil.gameDirectory);
        scriptingContainer.put("$_jrgss_paths", new String[]{FileUtil.localDirectory, FileUtil.gameDirectory});
        loadScriptData(ini.getScripts());
        //loadScriptsFromDirectory("/Users/matt/VidarScripts");

        //Gdx.app.log("JRGSSGame", scriptingContainer.runScriptlet("load_data(\"Data/Map101.rvdata2\")").toString());
    }

    @Override
    public JRGSSMain getMain() {
        return mainBlock;
    }


    public static interface JRGSSMain {
        public void main();
    }


}
