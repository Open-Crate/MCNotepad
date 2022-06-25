package com.opencratesoftware.mcnotepad;

public class FunctionResult 
{

    public FunctionResult(boolean inSuccess, String inUserFriendlyErrorMessage)
    {
        success = inSuccess;
        userFriendlyErrorMessage = inUserFriendlyErrorMessage;
    }

    public FunctionResult(boolean inSuccess, String inUserFriendlyMessage, String inIdentifier)
    {
        success = inSuccess;
        userFriendlyMessage = inUserFriendlyMessage;
        identifier = inIdentifier;
    }

    public boolean successful()
    {
        return success;
    }

    public String getUserFriendlyMessage()
    {
        return userFriendlyMessage;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    /* Whether or not this function succeeded in it's goal */
    boolean success = false;

    /* Message that is human readable and safe to make public */
    String userFriendlyMessage = "";  

    /* String to identify the type of error (Ex. "fileException") */
    String identifier = "";
}
