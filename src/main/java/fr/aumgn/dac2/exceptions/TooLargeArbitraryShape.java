package fr.aumgn.dac2.exceptions;

import fr.aumgn.dac2.DAC;

/**
 * Thrown when trying to define an arbitrary shape which is too large.
 */
public class TooLargeArbitraryShape extends DACException {

    private static final long serialVersionUID = 6526160450722634895L;

    public TooLargeArbitraryShape(DAC dac) {
        super(dac.getMessages().get("arbitraryshape.toolarge"));
    }
}
