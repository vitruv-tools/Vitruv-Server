package edu.neu.ccs.prl.galette.reactions.mir;

import tools.vitruv.change.interaction.UserInteractionOptions;

public class PositiveDoubleValidator implements UserInteractionOptions.InputValidator {

  public static PositiveDoubleValidator getPositiveDoubleValidatorInstance(){
    return new PositiveDoubleValidator();
  }

  @Override
  public String getInvalidInputMessage(String s) {
    return "Please input a positive, valid double instead of \"" + s + "\"";
  }

  @Override
  public boolean isInputValid(String s) {
    try {
      return Double.parseDouble(s) >= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
