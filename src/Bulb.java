import java.util.HashMap;

import processing.core.PApplet;


public class Bulb extends PApplet {
    static final int WINDOW_SIZE = 500;
    
    static final int BULB_RADIUS = WINDOW_SIZE / 4;
    static final int BULB_DIAMETER = WINDOW_SIZE / 2;
    static final int BULB_CENTER_X = WINDOW_SIZE / 2;
    static final int BULB_CENTER_Y = (WINDOW_SIZE * 2) / 5;
    
    static final int BULB_BASE_WIDTH = BULB_DIAMETER / 5 * 2;
    static final int BULB_BASE_HEIGHT = BULB_DIAMETER / 5 + BULB_DIAMETER / 25;
    
    static final int BULB_BASE_SPIRAL_WIDTH = BULB_RADIUS * 9 / 10;
    static final int BULB_BASE_SPIRAL_WEIGHT = BULB_DIAMETER / 25 * 3 / 2;
    static final int BULB_BASE_SPIRAL_EDGE_RAGUID = BULB_BASE_SPIRAL_WEIGHT / 2;
    
    static final int BULB_BASE_BOTTOM_HEIGHT = BULB_BASE_HEIGHT / 3;
    
    static final int BACKGROUND_COLOR = 20;
    static final int BULB_GLASS_EDGE_COLOR = 200;
    static final int BULB_GLASS_CENTER_COLOR = 255;
    static final int BULB_BASE_COLOR = 180;
    static final int BULB_BASE_SPIRAL_COLOR = 200;
    static final int BULB_BASE_BOTTOM_COLOR = 70;
    static final int DELAY = 8;
    
    static final String dm_name = Bulb.class.getSimpleName();
    static final String[] df_list = new String[]{"Scale"};
    static final HashMap<String, Float> feature_map = new HashMap<String, Float>();
    
    float current_scale;
    
    static public void write (String feature, float para_data) {
        if (!feature_map.containsKey(feature)) {
            // error
            return;
        }
        feature_map.put(feature, para_data);
    }

    public void setup() {
        size(WINDOW_SIZE, WINDOW_SIZE);
        background(BACKGROUND_COLOR);
        feature_map.put("Scale", 0f);
        current_scale = 0;
        DAI.init(dm_name, df_list);
    }

    public void draw() {
//        smooth();
        stroke(0, 0);
        noFill();
        
        current_scale += (feature_map.get("Scale") - current_scale) / DELAY;

        for (int r = BULB_DIAMETER; r > 0; r--) {
            float gradient_rate = pow((float) (BULB_DIAMETER - r) / BULB_DIAMETER, 0.25f);
            float gray_level = (BULB_GLASS_CENTER_COLOR - BULB_GLASS_EDGE_COLOR) * gradient_rate + BULB_GLASS_EDGE_COLOR;
            fill(gray_level, gray_level, gray_level * (1 - current_scale));
            ellipse(BULB_CENTER_X, BULB_CENTER_Y, r, r);
        }
        
        fill(BULB_BASE_COLOR);
        rectMode(CENTER);
        rect(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT / 2,
                BULB_BASE_WIDTH, BULB_BASE_HEIGHT
        );
        
        fill(BULB_BASE_SPIRAL_COLOR);
        rect(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS,
                BULB_BASE_SPIRAL_WIDTH, BULB_BASE_SPIRAL_WEIGHT,
                BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID);
        rect(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT / 3,
                BULB_BASE_SPIRAL_WIDTH, BULB_BASE_SPIRAL_WEIGHT,
                BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID);
        rect(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT * 2 / 3,
                BULB_BASE_SPIRAL_WIDTH, BULB_BASE_SPIRAL_WEIGHT,
                BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID, BULB_BASE_SPIRAL_EDGE_RAGUID);
        triangle(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT + BULB_BASE_BOTTOM_HEIGHT,
                BULB_CENTER_X - BULB_BASE_WIDTH / 2, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT,
                BULB_CENTER_X + BULB_BASE_WIDTH / 2, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT
        );
        
        fill(BULB_BASE_BOTTOM_COLOR);
        arc(
                BULB_CENTER_X, BULB_CENTER_Y + BULB_RADIUS + BULB_BASE_HEIGHT + BULB_BASE_BOTTOM_HEIGHT * 2 / 3,
                BULB_BASE_WIDTH / 3, BULB_BASE_BOTTOM_HEIGHT * 2 / 3 + 1,
                0, PI, CHORD
        );
        //fill(128);
        //stroke(128);
        //rectMode(CENTER);
        //rect(250,325,150,25);
        //rect(250,340,100,25);
        //rect(250,350,80,25);
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { "Bulb" });
    }
}
