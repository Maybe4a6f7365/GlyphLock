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

    @Test
    public void aNewEventMorphsWithoutReturningToAmbient() {
        ExperienceController controller = new ExperienceController();
        controller.reveal(0L);
        controller.frame(3_000L);
        controller.transitionEvent(4_000L);

        ExperienceController.Frame start = controller.frame(4_000L);
        ExperienceController.Frame middle = controller.frame(4_750L);
        ExperienceController.Frame complete = controller.frame(5_600L);

        assertEquals(1f, start.revealProgress, 0.0001f);
        assertEquals(0f, start.eventProgress, 0.0001f);
        assertTrue(middle.eventProgress > 0f && middle.eventProgress < 1f);
        assertEquals(1f, complete.eventProgress, 0.0001f);
        assertEquals(ExperienceController.State.FOCUSED, complete.state);
    }

    @Test
    public void frameBudgetDropsAfterBriefOperationalTail() {
        ExperienceController controller = new ExperienceController();
        ExperienceController.Frame ambient = controller.frame(10_000L);
        assertEquals(1000, ambient.frameDelayMs);
        assertTrue(!ambient.needsAnimation);

        controller.reveal(10_000L);
        ExperienceController.Frame revealing = controller.frame(10_300L);
        assertEquals(33, revealing.frameDelayMs);
        assertTrue(revealing.needsAnimation);

        controller.frame(13_000L);
        ExperienceController.Frame settling = controller.frame(13_100L);
        assertEquals(66, settling.frameDelayMs);
        assertTrue(settling.needsAnimation);

        ExperienceController.Frame focused = controller.frame(19_301L);
        assertEquals(1000, focused.frameDelayMs);
        assertTrue(!focused.needsAnimation);
    }

}
