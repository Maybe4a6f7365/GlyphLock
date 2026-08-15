package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExperienceControllerTest {
    @Test
    public void revealUsesVisibleMorphWindowBeforeFocus() {
        ExperienceController controller = new ExperienceController();
        controller.reveal(1_000L);

        ExperienceController.Frame early = controller.frame(1_500L);
        ExperienceController.Frame middle = controller.frame(2_225L);
        ExperienceController.Frame complete = controller.frame(3_600L);

        assertEquals(ExperienceController.State.REVEALING, early.state);
        assertTrue(early.revealProgress > 0f && early.revealProgress < 0.25f);
        assertTrue(middle.revealProgress > early.revealProgress);
        assertEquals(1f, complete.revealProgress, 0.0001f);
    }

    @Test
    public void collapseReturnsTheSameTopologyToAmbient() {
        ExperienceController controller = new ExperienceController();
        controller.reveal(0L);
        controller.frame(3_000L);
        controller.collapse(4_000L);

        ExperienceController.Frame middle = controller.frame(4_950L);
        ExperienceController.Frame complete = controller.frame(6_000L);

        assertTrue(middle.revealProgress > 0f && middle.revealProgress < 1f);
        assertEquals(ExperienceController.State.AMBIENT, complete.state);
        assertEquals(0f, complete.revealProgress, 0.0001f);
    }
}
