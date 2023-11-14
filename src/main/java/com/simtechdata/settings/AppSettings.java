package com.simtechdata.settings;

public class AppSettings {

    public static final Get get = Get.INSTANCE;
    public static final Set set = Set.INSTANCE;
    public static final Clear clear = Clear.INSTANCE;

    /*
        It's important to understand how these classes work together.
        First, a setting name is placed in the LABEL class with each method being able to respond to each name.
        Next, the Clear, Set and Get classes each get a method where a given LABEL shares the exact same method
        name. Any method in the Set class must first call its own name from the Clear class.

        The purpose for all of this is so that Application com.simtechdata.settings are both easy to write and read.
        Ex:
            AppSettings.SET.settingName(value);
            AppSettings.GET.settingName();
     */

}
