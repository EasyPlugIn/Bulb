import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import processing.core.PApplet;
@SuppressWarnings("serial")
public class DAI implements DAN.DAN2DAI {
	static DAI dai = new DAI();
	static IDA ida = new IDA();
    static DAN dan = new DAN();
	
	static abstract class DF {
        public DF (String name) {
            this.name = name;
        }
        public String name;
        public boolean selected;
    }
	
	static abstract class IDF extends DF {
        public IDF (String name) {
            super(name);
        }
    }
	
    static abstract class ODF extends DF {
        public ODF (String name) {
            super(name);
        }
        abstract public void pull(JSONArray data);
    }
    
    static abstract class Command {
        public Command(String name) {
            this.name = name;
        }
        public String name;
        abstract public void run(JSONArray dl_cmd_params, JSONArray ul_cmd_params);
    }
    
    static ArrayList<DF> df_list = new ArrayList<DF>();
    static ArrayList<Command> cmd_list = new ArrayList<Command>();
    static boolean suspended = true;
    static final String config_filename = "config.txt";
    
    static void add_df (DF... dfs) {
        for (DF df: dfs) {
            df_list.add(df);
        }
    }
    
    static void add_command (Command... cmds) {
        for (Command cmd: cmds) {
            cmd_list.add(cmd);
        }
    }
    
    private static boolean is_selected(String df_name) {
        for (DF df: df_list) {
            if (df_name.equals(df.name)) {
                return df.selected;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return false;
    }
 
    private static Command get_cmd(String cmd_name) {
        for(Command cmd: cmd_list) {
            if(cmd_name.equals(cmd.name) || cmd_name.equals(cmd.name + "_RSP")) {
                return cmd;
            }
        }
        System.out.println("Command" + cmd_name + "is not found");
        return null;
    }
    
    private static DF get_df(String df_name) {
        for(DF df: df_list) {
            if(df_name.equals(df.name)) {
                return df;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return null;
    }
    
    
    /* Default command-1: SET_DF_STATUS */
    static class SET_DF_STATUS extends Command {
        public SET_DF_STATUS() {
            super("SET_DF_STATUS");
        }
        public void run(final JSONArray df_status_list,
                         final JSONArray updated_df_status_list) {
            if(df_status_list != null && updated_df_status_list == null) {
            	final String flags = df_status_list.getString(0);
                for(int i = 0; i < flags.length(); i++) {
                    if(flags.charAt(i) == '0') {
                        df_list.get(i).selected = false;
                    } else {
                        df_list.get(i).selected = true;
                    }
                }
	            get_cmd("SET_DF_STATUS_RSP").run(
            		null,
            		new JSONArray(){{
		            	put(flags);
		            }}
        		);
            }
            else if(df_status_list == null && updated_df_status_list != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SET_DF_STATUS_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", updated_df_status_list);
    	                	}});
                		}}
            		);
            } else {
                System.out.println("Both the df_status_list and the updated_df_status_list are null");
            }
        }
    }
    /* Default command-2: RESUME */
    static class RESUME extends Command {
        public RESUME() {
            super("RESUME");
        }
        public void run(final JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = false;
                get_cmd("RESUME_RSP").run(
                	null, 
                	new JSONArray(){{
                	    put("OK");
	            }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("RESUME_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    /* Default command-3: SUSPEND */
    static class SUSPEND extends Command {
        public SUSPEND() {
            super("SUSPEND");
        }
        public void run(JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = true;
                get_cmd("SUSPEND_RSP").run(
                	null, 
                	new JSONArray(){{
                		put("OK");
    	        }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SUSPEND_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    
	public void add_shutdownhook() {
		Runtime.getRuntime().addShutdownHook(new Thread () {
            @Override
            public void run () {
            	deregister();
            }
        });
	}
	
	/* deregister() */
	public void deregister() {
		dan.deregister();
	}
	
	@Override
	public void pull(final String odf_name, final JSONArray data) {
		if (odf_name.equals("Control")) {
            final String cmd_name = data.getString(0);
            JSONArray dl_cmd_params = data.getJSONObject(1).getJSONArray("cmd_params");
            Command cmd = get_cmd(cmd_name);
            if (cmd != null) {
                cmd.run(dl_cmd_params, null);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_COMMAND");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(cmd_name);
            		}});
            	}});
            }});
        } else {
        	ODF odf = ((ODF)get_df(odf_name));
        	if (odf != null) {
        		odf.pull(data);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_ODF");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(odf_name);
            		}});
            	}});
            }});
        }
	}
	
	
   static private String get_config_ec () {
        try {
            /* assume that the config file has only one line,
             *  which is the IP address of the EC (without port number)*/
            BufferedReader br = new BufferedReader(new FileReader(config_filename));
            try {
                String line = br.readLine();
                if (line != null) {
                    return line;
                }
                return "localhost";
            } finally {
                br.close();
            }
        } catch (IOException e) {
            return "localhost";
        }
    }
	

    /* The main() function */
    public static void main(String[] args) {
        add_command(
            new SET_DF_STATUS(),
            new RESUME(),
            new SUSPEND()
        );
        init_cmds();
        init_dfs();
        final JSONArray df_name_list = new JSONArray();
        for(int i = 0; i < df_list.size(); i++) {
            df_name_list.put(df_list.get(i).name);
        }
        
        String endpoint = get_config_ec();
        if (!endpoint.startsWith("http://")) {
            endpoint = "http://" + endpoint;
        }
        if (endpoint.length() - endpoint.replace(":", "").length() == 1) {
            endpoint += ":9999";
        }
        
        JSONObject profile = new JSONObject() {{
            put("dm_name", dm_name); //deleted
            put("u_name", "yb");
            put("df_list", df_name_list);
            put("is_sim", false);
        }};
        
        String d_id = "";
        Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            d_id += "0123456789ABCDEF".charAt(a);
        }

        dan.init(dai, endpoint, d_id, profile);
        dai.add_shutdownhook();

        /* Performs the functionality of the IDA */
        ida.iot_app();             
    }
    
    /*--------------------------------------------------*/
    /* Customizable part */
    static String dm_name = "Bulb";
    
    static class Scale extends ODF {
        public Scale () {
            super("Scale");
        }
        @Override
        public void pull(JSONArray data) {
            IDA.write("Scale", (float)data.getDouble(0));
        }
    }
    
    /* Initialization of command list and DF list, generated by the DAC */
    static void init_cmds () {
        add_command(
        //    new SAMPLE_COMMAND ()
        );
    }
    static void init_dfs () {
        add_df(
    		new Scale()
        );
    }
    
    /* IDA Class */
    public static class IDA extends PApplet {
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
        }

        public void draw() {
//            smooth();
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
        
        public void iot_app() {
            PApplet.runSketch(new String[]{"Bulb"}, this);
        };
    }
}
