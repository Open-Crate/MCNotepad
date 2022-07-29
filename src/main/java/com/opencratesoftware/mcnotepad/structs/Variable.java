package com.opencratesoftware.mcnotepad.structs;

public class Variable 
{
    public String toString()
    {
        return Name + " " + Value;
    }

    public Variable(String inName, String inValue)
    {
        Name = inName;
        Value = inValue;
    }
    public String Name;
    public String Value;  
}
