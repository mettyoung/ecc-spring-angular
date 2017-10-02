package com.ecc.hibernate_xml.ui_handler.person;

import com.ecc.hibernate_xml.ui_handler.UiHandler;
import com.ecc.hibernate_xml.util.InputHandler;
import com.ecc.hibernate_xml.service.PersonService;
import com.ecc.hibernate_xml.model.Person;
import com.ecc.hibernate_xml.model.Name;

public class CreatePersonUiHandler extends UiHandler {

	private static final String LAST_NAME_PROMPT = "Please enter the last name: ";
	private static final String FIRST_NAME_PROMPT = "Please enter the first name: ";
	private static final String MIDDLE_NAME_PROMPT = "Please enter the middle name: ";

	private PersonService personService;

	public CreatePersonUiHandler(String operationName) {
		super(operationName);
		personService = new PersonService();
	}

	@Override 
	public void onHandle() throws Exception {
		Name name = new Name(
			InputHandler.getNextLine(LAST_NAME_PROMPT),
			InputHandler.getNextLine(FIRST_NAME_PROMPT),
			InputHandler.getNextLine(MIDDLE_NAME_PROMPT));

		personService.createPerson(new Person(name));

		System.out.println(String.format("Successfully created the person \"%s\"!", name));
	}

	@Override 
	protected Boolean relinquishControl() {
		return true;
	}
}