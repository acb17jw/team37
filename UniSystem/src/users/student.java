package users;
import java.util.Collection;

import university.Degree;
import university.Module;
import university.PeriodOfStudy;

public class Student extends User {

	private int registrationID;

	private Degree degree;
	
	private Collection<Module> module;

	public int getRegistrationID() {
		return 0;
	}

	public void viewStatus() {

	}

	public Person getID() {
		return null;
	}

	public PeriodOfStudy getPeriodOfStudy() {
		return null;
	}

	public void setPeriodOfStudy() {

	}

	@Override
	public boolean login() {
		// TODO Auto-generated method stub
		return false;
	}

}