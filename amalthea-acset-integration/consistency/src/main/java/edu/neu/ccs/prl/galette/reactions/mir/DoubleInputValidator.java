package edu.neu.ccs.prl.galette.reactions.mir;

import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;

public class DoubleInputValidator implements InputValidator {

  public static DoubleInputValidator getDoubleInputValidatorInstance(){
    return new DoubleInputValidator();
  }

  @Override
  public String getInvalidInputMessage(String s) {
    return "Please input a valid double instead of \"" + s + "\"";
  }

  @Override
  public boolean isInputValid(String s) {
    try {
      Double.parseDouble(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
