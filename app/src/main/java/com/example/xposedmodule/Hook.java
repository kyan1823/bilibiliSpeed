package com.example.xposedmodule;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals("com.example.demo")) return;
        hook(loadPackageParam);
    }

    private void hook(XC_LoadPackage.LoadPackageParam loadPackageParam){
        XposedBridge.log("XposedHook success");
    }
}
