package com.netifera.platform.tools.options;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.tools.IParsableOption;

public class MultipleStringOption extends Option implements IParsableOption {
	private static final long serialVersionUID = 4143081786593310092L;
	
	private String[] values;
	private String[] possibleValues;
	private String[] defaultSelection;

	public MultipleStringOption(String name, String label, String description, String[] values, String[] possibleValues) {
		super(name, label, description);
		this.possibleValues = possibleValues;
		this.defaultSelection = values;
		setToDefault();
	}
	
	public MultipleStringOption(String name, String label, String description, String[] possibleValues) {
		this(name, label, description, new String[0], possibleValues);
	}
	
	public String[] getValue() {
		return values;
	}

	public void setValue(String[] newValues) {
		this.values = newValues;
	}

	public String[] getDefault() {
		return defaultSelection;
	}
	
	public String[] getPossibleValues() {
		return possibleValues;
	}
	
	@Override
	public boolean isDefault() {
/*		if (values.size() != defaultSelection.length)
			return false;
		for (String value: defaultSelection)
			if (!values.contains(value))
		return Arrays.values.equals(defaultValue);
*/
		return false;
	}
	
	@Override
	public void setToDefault() {
		setValue(defaultSelection);
	}

	public boolean fromString(final String text) {
		List<String> valuesList = new ArrayList<String>();
		for (String value: text.split("[\\s,]+"))
			valuesList.add(value);
		values = valuesList.toArray(new String[valuesList.size()]);
		return true;
	}
}
