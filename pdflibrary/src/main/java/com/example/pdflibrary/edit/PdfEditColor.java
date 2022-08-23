package com.example.pdflibrary.edit;

import android.graphics.Color;

public enum PdfEditColor {

    BLUE() {
        @Override
        public int getColor() {
            return Color.parseColor("#643EC1FA");
        }

        @Override
        public String getStringColor() {
            return "#643EC1FA";
        }

        @Override
        public String getColorName() {
            return "蓝色";
        }
    },

    GREEN() {
        @Override
        public int getColor() {
            return Color.parseColor("#6400E79B");
        }

        @Override
        public String getStringColor() {
            return "#6400E79B";
        }

        @Override
        public String getColorName() {
            return "绿色";
        }
    },

    YELLOW() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FAE143");
        }

        @Override
        public String getStringColor() {
            return "#64FAE143";
        }

        @Override
        public String getColorName() {
            return "黄色";
        }
    },

    ORANGE() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FFB94E");
        }

        @Override
        public String getStringColor() {
            return "#64FFB94E";
        }

        @Override
        public String getColorName() {
            return "橙色";
        }
    },

    PINK() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FF5682");
        }

        @Override
        public String getStringColor() {
            return "#64FF5682";
        }

        @Override
        public String getColorName() {
            return "粉红色";
        }
    },

    PINK_LILAC() {
        @Override
        public int getColor() {
            return Color.parseColor("#64E76BEC");
        }

        @Override
        public String getStringColor() {
            return "#64E76BEC";
        }

        @Override
        public String getColorName() {
            return "粉紫色";
        }
    };

    public abstract int getColor();

    public abstract String getStringColor();

    public abstract String getColorName();
}
