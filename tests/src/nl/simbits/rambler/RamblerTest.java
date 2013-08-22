package nl.simbits.rambler;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class nl.simbits.rambler.RamblerTest \
 * nl.simbits.rambler.tests/android.test.InstrumentationTestRunner
 */
public class RamblerTest extends ActivityInstrumentationTestCase2<Rambler> {

    public RamblerTest() {
        super("nl.simbits.rambler", Rambler.class);
    }

}
