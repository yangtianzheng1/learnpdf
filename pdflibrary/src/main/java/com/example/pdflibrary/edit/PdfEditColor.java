package com.example.pdflibrary.edit;

import android.graphics.Color;

public enum PdfEditColor {

    BLUE() {
        @Override
        public int getColor() {
            return Color.parseColor("#643EC1FA");
        }
    },

    GREEN() {
        @Override
        public int getColor() {
            return Color.parseColor("#6400E79B");
        }
    },

    YELLOW() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FAE143");
        }
    },

    ORANGE() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FFB94E");
        }
    },

    PINK() {
        @Override
        public int getColor() {
            return Color.parseColor("#64FF5682");
        }
    },

    PINK_LILAC() {
        @Override
        public int getColor() {
            return Color.parseColor("#64E76BEC");
        }
    };

    public abstract int getColor();
}
