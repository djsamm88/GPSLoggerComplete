/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.ui.components;


import androidx.annotation.Nullable;
import com.mendhak.gpslogger.R;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;


public class GpsLoggerDrawerItem {


    public static PrimaryDrawerItem newPrimary(int resTitle, @Nullable Integer resSummary, int resIcon, int identifier) {

        PrimaryDrawerItem pdi = new PrimaryDrawerItem()
                .withName(resTitle)
                .withIcon(resIcon)
                .withIdentifier(identifier)
                .withDescriptionTextColorRes(R.color.secondaryColorText)
                .withSelectable(false);

        if (resSummary != null) {
            pdi = pdi.withDescription(resSummary);
        }
        return pdi;

    }

//    public static SecondaryDrawerItem newSecondary(int resTitle, int resIcon, int identifier) {
//
//        return new SecondaryDrawerItem()
//                .withName(resTitle)
//                .withIcon(resIcon)
//                .withIdentifier(identifier)
//                .withTextColorRes(Systems.isDarkMode(AppSettings.getInstance().getApplicationContext()) ? R.color.primaryColorLight : R.color.primaryColorText)
//                .withDescriptionTextColorRes(R.color.secondaryColorText)
//                .withSelectable(false);
//
//    }


}

