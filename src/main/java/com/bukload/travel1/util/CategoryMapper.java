package com.bukload.travel1.util;

import java.util.Set;

public final class CategoryMapper {
    private CategoryMapper() {}

    // Google types -> 우리 ENUM
    public static String fromGoogleTypes(Set<String> types) {
        if (types == null) return null;
        if (types.contains("cafe")) return "CAFE";
        if (types.contains("restaurant") || types.contains("food") || types.contains("meal_takeaway") || types.contains("meal_delivery"))
            return "FOOD";
        if (types.contains("lodging")) return "STAY";
        if (types.contains("tourist_attraction") || types.contains("park") || types.contains("museum")
                || types.contains("point_of_interest") || types.contains("establishment"))
            return "LAND MARK";
        return null; // 매핑 불가 시 null
    }

    // Kakao category_group_code -> 우리 ENUM
    public static String fromKakaoGroup(String code) {
        if (code == null) return null;
        switch (code) {
            case "CE7": return "CAFE";
            case "FD6": return "FOOD";
            case "AD5": return "STAY";
            case "AT4": return "LAND MARK";
            default: return null;
        }
    }
}
