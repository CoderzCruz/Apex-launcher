package net.kdt.pojavlaunch.game.renderer.impl;

import android.content.Context;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;

import java.util.Map;

/**
 * Cosmic RenderX - Vortex's branded Vulkan renderer. It rides the proven mesa/Zink
 * GL-to-Vulkan path (same device requirements as Zink) and adds a lower-latency WSI
 * present mode so frame delivery stays ahead of slow GL translators.
 */
public class CosmicRenderSpec extends MesaRenderSpec.ZinkRenderSpec {
    @Override
    public String name() {
        return "COSMIC RENDERX";
    }

    @Override
    public String tag() {
        return Renderers.COSMIC_RENDERX_RENDERER;
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_cosmic_renderx;
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        super.setupEnvironment(context, envMap);
        // Mailbox present mode: lower latency than FIFO on mobile drivers.
        envMap.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
    }
}
