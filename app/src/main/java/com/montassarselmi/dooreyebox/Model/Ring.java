package com.montassarselmi.dooreyebox.Model;


import android.content.res.Resources;

import com.montassarselmi.dooreyebox.R;

public class Ring extends EventHistory {

    private String visitorImage;
    private int id;
    private String time;
    private String status;
    private String responder;
    public Ring(){
        super();
    }

    public Ring(int id, String time, String responder, String visitorImage)
    {
        super(id,time, "Ring",responder, visitorImage);

    }
    public Ring(int id, String time)
    {
        super(id,time,  "Ring", "No one", null);
    }
    public Ring(int id, String time, String visitorImage)
    {
        super(id,time,  "Ring", "No one",visitorImage);
    }


}
