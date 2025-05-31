package com.hook.bilibili.speed;

import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed模块主类，用于Hook哔哩哔哩播放器并修改播放速度
 */
public class MainHook implements IXposedHookLoadPackage {
    // 共享偏好设置，用于存储和读取播放速度配置
    private static final XSharedPreferences prefs = new XSharedPreferences("com.hook.bilibili.speed", "speed");
    // 默认播放速度
    private static final float DEFAULT_SPEED = 1.0f;

    private static XC_MethodHook.Unhook hookNotifyOnInfo = null;
    private static XC_MethodHook.Unhook hookOnPreparedListener_OnPrepared = null;
    private static XC_MethodHook.Unhook hookFinalOnPreparedListener_onPrepared = null;

    /**
     * 从共享偏好设置中获取播放速度配置
     *
     * @return 当前设置的播放速度
     */
    private static float getSpeedConfig() {
        prefs.reload();
        return prefs.getFloat("speed", DEFAULT_SPEED);
    }

    /**
     * 检查播放速度配置是否发生变化
     *
     * @return true如果配置已更改，false否则
     */
    private static boolean hasSpeedConfigChanged() {
        return prefs.hasFileChanged();
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理哔哩哔哩
        if(!lpparam.packageName.equals("tv.danmaku.bili")){
            return;
        }
        hookBilibiliPlayer(lpparam);
    }

    /**
     * Hook哔哩哔哩播放器相关方法
     *
     * @param lpparam Xposed模块加载参数
     */
    private void hookBilibiliPlayer(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookNotifyOnInfo != null) return;
//        XposedBridge.log("XposedHook load package successfully: " + lpparam.packageName);

        // Hook AbstractMediaPlayer的notifyOnInfo方法作为入口点
        hookNotifyOnInfo = XposedHelpers.findAndHookMethod(
                "tv.danmaku.ijk.media.player.AbstractMediaPlayer",
                lpparam.classLoader,
                "notifyOnInfo",
                int.class,
                int.class,
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws IllegalAccessException {
                        if (hookOnPreparedListener_OnPrepared != null) return;
                        hookOnPreparedListener(param, lpparam);
                    }
                });
//        XposedBridge.log("Have hooked AbstractMediaPlayer -> notifyOnInfo");
    }

    /**
     * Hook OnPreparedListener相关方法
     *
     * @param param   方法Hook参数
     * @param lpparam Xposed模块加载参数
     */
    private void hookOnPreparedListener(XC_MethodHook.MethodHookParam param, XC_LoadPackage.LoadPackageParam lpparam)
            throws IllegalAccessException {
        // 获取mOnPreparedListener字段
        Field mOnPreparedListener = XposedHelpers.findField(param.thisObject.getClass(), "mOnPreparedListener");
        Class<?> listenerClass = mOnPreparedListener.get(param.thisObject).getClass();
//        XposedBridge.log("Find field mOnPreparedListener in AbstractMediaPlayer");

        // Hook OnPreparedListener的onPrepared方法
        hookOnPreparedListener_OnPrepared = XposedHelpers.findAndHookMethod(
                listenerClass,
                "onPrepared",
                "tv.danmaku.ijk.media.player.IMediaPlayer",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws IllegalAccessException {
                        if (hookFinalOnPreparedListener_onPrepared != null) return;
                        hookPlayerInstance(param, lpparam);
                    }
                });
//        XposedBridge.log("Have hooked mOnPreparedListener -> onPrepared");
    }

    /**
     * Hook播放器实例相关方法
     *
     * @param param   方法Hook参数
     * @param lpparam Xposed模块加载参数
     */
    private void hookPlayerInstance(XC_MethodHook.MethodHookParam param, XC_LoadPackage.LoadPackageParam lpparam)
            throws IllegalAccessException {
        // 获取播放器实例
        Field[] fields = param.thisObject.getClass().getDeclaredFields();
//        XposedBridge.log("Found fields count: " + fields.length);

        Field playerField = fields[0];
        if (playerField == null) {
            XposedBridge.log("playerField is null");
            return;
        }

        playerField.setAccessible(true);
        Object playerInstance = playerField.get(param.thisObject);
        Class<?> playerClass = playerInstance.getClass();
//        XposedBridge.log("Chosen field : " + playerClass);

        // 获取OnPreparedListener接口类
        Class<?> onPreparedListenerInterface = XposedHelpers.findClass(
                "tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener",
                lpparam.classLoader);

        // 在类继承链中查找OnPreparedListener字段
        Class<?> currentClass = playerClass;
        do {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.getType() == onPreparedListenerInterface && !Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    Class<?> preparedListenerClass = field.get(playerInstance).getClass();
//                    XposedBridge.log(" Found field OnPreparedListener: " + preparedListenerClass);

                    // Hook最终的OnPreparedListener
                    hookFinalOnPreparedListener(preparedListenerClass);
                    return;
                }
            }
        } while ((currentClass = currentClass.getSuperclass()) != null);
        XposedBridge.log("Cannot find field OnPreparedListener in chosen class: " + playerClass);
    }

    /**
     * Hook最终的OnPreparedListener并设置播放速度
     *
     * @param listenerClass OnPreparedListener类
     */
    private void hookFinalOnPreparedListener(Class<?> listenerClass) {
        hookFinalOnPreparedListener_onPrepared = XposedHelpers.findAndHookMethod(
                listenerClass,
                "onPrepared",
                "tv.danmaku.ijk.media.player.IMediaPlayer",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws IllegalAccessException {
                        setupPlaybackSpeedControl(param);
                    }
                });
//        XposedBridge.log("Hooked method onPrepared");
    }

    /**
     * 设置播放速度控制
     *
     * @param param 方法Hook参数
     */
    private void setupPlaybackSpeedControl(XC_MethodHook.MethodHookParam param) throws IllegalAccessException {
        // 获取播放器控制实例
        Field[] fields = param.thisObject.getClass().getDeclaredFields();
//        XposedBridge.log("Found fields count: " + fields.length);
        Field controlField = fields[0];
        controlField.setAccessible(true);
        Class<?> controlClass = controlField.get(param.thisObject).getClass();
//        XposedBridge.log("Chosen field: " + controlClass);
        // 查找设置播放速度的方法
        for (Method method : controlClass.getDeclaredMethods()) {
            if (isSetSpeedMethod(method)) {
//                XposedBridge.log( "Found method to setup the speed: " + method);
                // 初始化速度配置
                final float[] currentSpeed = {getSpeedConfig()};
                // Hook设置速度方法
                hookSetSpeedMethod(method, currentSpeed);
                // Hook resume方法以应用配置的速度
                hookResumeMethod(controlClass, method, currentSpeed);
                XposedBridge.log("Hook finished");
                hookNotifyOnInfo.unhook();
                hookOnPreparedListener_OnPrepared.unhook();
                hookFinalOnPreparedListener_onPrepared.unhook();
                break;
            }
        }
        XposedBridge.log("Cannot find the method to setup the speed");
    }

    /**
     * 检查方法是否为设置播放速度的方法
     *
     * @param method 待检查的方法
     * @return true如果是设置速度方法，false否则
     */
    private boolean isSetSpeedMethod(Method method) {
        return void.class == method.getReturnType() &&
                method.getParameterCount() == 1 &&
                float.class == method.getParameterTypes()[0];
    }

    /**
     * Hook设置播放速度的方法
     *
     * @param method       设置速度的方法
     * @param currentSpeed 当前速度配置的引用
     */
    private void hookSetSpeedMethod(Method method, final float[] currentSpeed) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                // 检查是否是用户手动调整速度
                if ((float) param.args[0] != currentSpeed[0]) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (int i = 4; i < stackTrace.length; i++) {
                        if (stackTrace[i].getClassName().equals("com.bilibili.player.tangram.basic.PlaySpeedManagerImpl")) {
//                            XposedBridge.log("User changed the speed manually: " + param.args[0]);
                            currentSpeed[0] = (float) param.args[0];
                            return;
                        }
                    }
                }
            }
        });
//        XposedBridge.log("Have hooked method to setup the speed");
    }

    /**
     * Hook resume方法以应用配置的速度
     *
     * @param controlClass   控制类
     * @param setSpeedMethod 设置速度的方法
     * @param currentSpeed   当前速度配置的引用
     */
    private void hookResumeMethod(Class<?> controlClass, final Method setSpeedMethod, final float[] currentSpeed) {
        XposedHelpers.findAndHookMethod(controlClass, "resume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                // 检查配置是否变化
                if (hasSpeedConfigChanged()) {
//                    XposedBridge.log("config changed: " + currentSpeed[0]);
                    currentSpeed[0] = getSpeedConfig();
                }
                // 应用当前速度
                try {
                    setSpeedMethod.invoke(param.thisObject, currentSpeed[0]);
                    XposedBridge.log("set speed: " + currentSpeed[0]);
                } catch (Exception e) {
                    XposedBridge.log("Exception: " + e);
                }
            }
        });
//        XposedBridge.log("Have hooked method resume");
    }
}

