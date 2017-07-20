package com.joedobo27.scm;

import com.wurmonline.client.game.SeasonManager;
import javassist.*;

import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Logger;


public class SeasonConfigureMod implements WurmClientMod, Configurable, Initable {

    private static boolean summerAlways = false;
    private static boolean fallAlways = false;
    private static boolean winterAlways = false;
    private static boolean springAlways = false;
    private static float averageTemperature = 9.0f;
    private static float summerWinterDeltaT = 24.0f;
    private static final Logger logger = Logger.getLogger(SeasonConfigureMod.class.getName());

    @Override
    public void configure(Properties properties) {
        summerAlways = Boolean.parseBoolean(properties.getProperty("summerAlways", Boolean.toString(summerAlways)));
        fallAlways = Boolean.parseBoolean(properties.getProperty("fallAlways", Boolean.toString(fallAlways)));
        winterAlways = Boolean.parseBoolean(properties.getProperty("winterAlways", Boolean.toString(winterAlways)));
        springAlways = Boolean.parseBoolean(properties.getProperty("springAlways", Boolean.toString(springAlways)));
        averageTemperature = Float.parseFloat(properties.getProperty("averageTemperature", Float.toString(averageTemperature)));
        summerWinterDeltaT = Float.parseFloat(properties.getProperty("summerWinterDeltaT", Float.toString(summerWinterDeltaT)));
        if (summerWinterDeltaT < 0f){
            summerWinterDeltaT = 0.0f;
        }
    }

    @Override
    public void init() {
        if (summerAlways || fallAlways || winterAlways || springAlways) {
            useFixedSeason();
            return;
        }
        configureSeason();
    }

    /**
     * If one of the mod's fixed season boolean fields is true this method overwrites
     * {@link SeasonManager#selectSeason(float, float)} to always return a value which makes it that season.
     */
    @SuppressWarnings("ConstantConditions")
    private void useFixedSeason() {
        try {
            CtClass ctClassSeasonManager = HookManager.getInstance().getClassPool().get("com.wurmonline.client.game.SeasonManager");
            CtClass returnType = HookManager.getInstance().getClassPool().get("com.wurmonline.client.game.SeasonManager$Season");
            CtClass[] paramTypes = {CtPrimitiveType.floatType, CtPrimitiveType.floatType};
            CtMethod ctMethodSelectSeason = ctClassSeasonManager.getMethod("selectSeason", Descriptor.ofMethod(returnType, paramTypes));
            if (summerAlways) {
                logger.info("season override: Summer " + summerAlways);
                ctMethodSelectSeason.setBody("{com.wurmonline.client.game.SeasonManager.Season newSeason;" +
                        "newSeason = com.wurmonline.client.game.SeasonManager.Season.SUMMER;return newSeason;}");
            } else if (fallAlways) {
                logger.info("season override: Fall " + fallAlways);
                ctMethodSelectSeason.setBody("{com.wurmonline.client.game.SeasonManager.Season newSeason;" +
                        "newSeason = com.wurmonline.client.game.SeasonManager.Season.FALL;return newSeason;}");
            } else if (winterAlways) {
                logger.info("season override: Winter " + winterAlways);
                ctMethodSelectSeason.setBody("{com.wurmonline.client.game.SeasonManager.Season newSeason;" +
                        "newSeason = com.wurmonline.client.game.SeasonManager.Season.WINTER;return newSeason;}");
            } else if (springAlways) {
                logger.info("season override: Spring " + springAlways);
                ctMethodSelectSeason.setBody("{com.wurmonline.client.game.SeasonManager.Season newSeason;" +
                        "newSeason = com.wurmonline.client.game.SeasonManager.Season.SPRING;return newSeason;}");
            }
        }catch (NotFoundException | CannotCompileException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }

    /**
     * Install a HookManager hook into {@link SeasonManager#initialize(long, float)} so {@link SeasonManager#averageTemperature}
     * and {@link SeasonManager#summerWinterDeltaT} fields are set with the values specified by this mod. Method initialize() only
     * runs once to setup so reflection overhead is negligible.
     */
    private void configureSeason() {
        HookManager.getInstance().registerHook("com.wurmonline.client.game.SeasonManager", "initialize",
                null, () -> (proxy, method, args) -> {
            // Using the invocationHandler.Object in order to run reflection utilities before method initialize.
            try {
                @SuppressWarnings("unchecked") Class<SeasonManager> c = (Class<SeasonManager>) proxy.getClass();
                Field fieldAverageTemperature  =  c.getDeclaredField("averageTemperature");
                fieldAverageTemperature.setAccessible(true);
                fieldAverageTemperature.setFloat(proxy, averageTemperature);
                fieldAverageTemperature.setAccessible(false);
                Field fieldSummerWinterDeltaT = c.getDeclaredField("summerWinterDeltaT");
                fieldSummerWinterDeltaT.setAccessible(true);
                fieldSummerWinterDeltaT.setFloat(proxy, summerWinterDeltaT);
                fieldSummerWinterDeltaT.setAccessible(false);
            }catch (NoSuchFieldException | IllegalAccessException e) {
                logger.warning(e.getMessage());
            }
            return method.invoke(proxy, args);
        });
    }
}