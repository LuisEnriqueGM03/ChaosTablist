package com.chaos.tablist.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
/** 1.20.1 equivalent of the later GUI sprite atlas, retaining nine-slice borders. */
public final class LegacyGui {
    private LegacyGui() {}
    public static void blitSprite(GuiGraphics g, ResourceLocation id, int x,int y,int w,int h) {
        RenderSystem.enableBlend();
        if(id.getNamespace().equals("minecraft") && id.getPath().startsWith("icon/ping_")) {
            String key=id.getPath().substring(10);
            int row=key.equals("unknown") ? 5 : 5-Integer.parseInt(key);
            g.blit(new ResourceLocation("textures/gui/icons.png"),x,y,0,176+row*8,10,8);
            return;
        }
        int tw,th,b;
        switch(id.getPath()) {
            case "button" -> { tw=20; th=20; b=3; }
            case "button_disabled" -> { tw=20; th=20; b=3; }
            case "button_hover" -> { tw=20; th=20; b=3; }
            case "button_selected" -> { tw=20; th=20; b=3; }
            case "cell" -> { tw=16; th=16; b=2; }
            case "header" -> { tw=16; th=14; b=3; }
            case "inset" -> { tw=16; th=16; b=2; }
            case "panel" -> { tw=48; th=48; b=8; }
            default -> throw new IllegalArgumentException("Unknown GUI sprite " + id);
        }
        ResourceLocation texture=new ResourceLocation(id.getNamespace(),"textures/gui/sprites/"+id.getPath()+".png");
        int bx=Math.min(b,w/2),by=Math.min(b,h/2);
        int[] dx={x,x+bx,x+w-bx},dy={y,y+by,y+h-by};
        int[] dw={bx,w-2*bx,bx},dh={by,h-2*by,by};
        int[] sx={0,b,tw-b},sy={0,b,th-b},sw={b,tw-2*b,b},sh={b,th-2*b,b};
        for(int i=0;i<3;i++) for(int j=0;j<3;j++) if(dw[i]>0 && dh[j]>0)
            g.blit(texture,dx[i],dy[j],dw[i],dh[j],(float)sx[i],(float)sy[j],sw[i],sh[j],tw,th);
    }
}
