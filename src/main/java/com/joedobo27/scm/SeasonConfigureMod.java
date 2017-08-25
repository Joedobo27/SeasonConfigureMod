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

    private static boolean isSummerAlways = false;
    private static boolean isFallAlways = false;
    private static boolean isWinterAlways = false;
    private static boolean isSpringAlways = false;
    private static float averageTemperature = 9.0f;
    private static float summerWinterDeltaT = 24.0f;
    private static final Logger logger = Logger.getLogger(SeasonConfigureMod.class.getName());

    @Override
    public void configure(Properties properties) {
        isSummerAlways = Boolean.parseBoolean(properties.getProperty("isSummerAlways", Boolean.toString(isSummerAlways)));
        isFallAlways = Boolean.parseBoolean(properties.getProperty("isFallAlways", Boolean.toString(isFallAlways)));
        isWinterAlways = Boolean.parseBoolean(properties.getProperty("isWinterAlways", Boolean.toString(isWinterAlways)));
        isSpringAlways = Boolean.parseBoolean(properties.getProperty("isSpringAlways", Boolean.toString(isSpringAlways)));
        averageTemperature = Float.parseFloat(properties.getProperty("averageTemperature", Float.toString(averageTemperature)));
        summerWinterDeltaT = Float.parseFloat(properties.getProperty("summerWinterDeltaT", Float.toString(summerWinterDeltaT)));
        if (summerWinterDeltaT < 0f){
            summerWinterDeltaT = 0.0f;
        }
    }

    @Override
    public void init() {
        if (isSummerAlways || isFallAlways || isWinterAlways || isSpringAlways) {
            useFixedSeason();
            if (isSummerAlways)
                logger.info("Season will be overridden to always show summer graphics.");
            else if (isFallAlways)
                logger.info("Season will be overridden to always show fall graphics.");
            else if (isWinterAlways)
                logger.info("Season will be overridden to always show winter graphics.");
            else if (isSpringAlways)
                logger.info("Season will be overridden to always show spring graphics.");
            return;
        }
        configureSeason();
    }

    /**
     * Hook into {@link SeasonManager#selectSeason(float, float)} and change its return value if one of the isSeason
     * values are true.
     */
    private void useFixedSeason() {

        CtClass seasonCtClass = null;
        try {
            seasonCtClass = HookManager.getInstance().getClassPool().get("com.wurmonline.client.game.SeasonManager$Season");
        } catch (NotFoundException nfe) {
            logger.warning(nfe.getMessage());
        }

        CtClass returnType = seasonCtClass;
        CtClass[] paramTypes = {
                CtPrimitiveType.floatType,
                CtPrimitiveType.floatType
        };
        HookManager.getInstance().registerHook("com.wurmonline.client.game.SeasonManager", "selectSeason",
                Descriptor.ofMethod(returnType, paramTypes), () -> (proxy, method, args) -> {
                    SeasonManager.Season season = (SeasonManager.Season) method.invoke(proxy, args);
                    if (isSummerAlways) {
                        season = SeasonManager.Season.SUMMER;
                    } else if (isFallAlways) {
                        season = SeasonManager.Season.FALL;
                    } else if (isWinterAlways) {
                        season = SeasonManager.Season.WINTER;
                    } else if (isSpringAlways) {
                        season = SeasonManager.Season.SPRING;
                    }
                    return season;
                });
    }

    /**
     * Install a HookManager hook into {@link SeasonManager#initialize(long, float)} so {@link SeasonManager#averageTemperature}
     * and {@link SeasonManager#summerWinterDeltaT} fields are set with the values specified by this mod. Method initialize() only
     * runs once to setup so reflection overhead is negligible.
     */
    private void configureSeason() {
        CtClass returnType = CtPrimitiveType.voidType;
        CtClass[] paramTypes = {
                CtPrimitiveType.longType,
                CtPrimitiveType.floatType
        };

        HookManager.getInstance().registerHook("com.wurmonline.client.game.SeasonManager", "initialize",
                Descriptor.ofMethod(returnType, paramTypes), () -> (proxy, method, args) -> {
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

        logger.info("averageTemperature overridden to "+averageTemperature);
        logger.info("summerWinterDeltaT overridden to "+summerWinterDeltaT);
    }
}