package com.opencratesoftware.mcnotepad.structs;

public class Variable 
{
    public String toString()
    {
        if (!Name.equals(""))
        {
            return Name + " " + Value;
        }
        else
        {
            return "";
        }
    }

    public Variable(String inName, String inValue)
    {
        Name = inName;
        Value = inValue;
    }
    public String Name;
    public String Value;  
}
