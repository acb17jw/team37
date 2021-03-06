package users;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import university.Degree;
import university.Module;
import database.SqlDriver;


public class Registrar extends User {
//set the e-mail domain
String universityDomain = "@hogwart.ac.uk";	

public static void infoBox(String infoMessage, String titleBar) {
	JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
}	
	
SqlDriver sqldriver = new SqlDriver();

	public boolean addStudent( Student student, String periodOfStudy)
	{	//get student's data
		String username = student.getUsername();
		String title = student.getTitle();
		String forname = student.getForename();
		String surname = student.getSurname();
		String personalTutor = student.getPersonalTutor();
		String email = student.getEmail();
		Degree degree = student.getDegree();

		int registrationNum = student.getRegistrationID();
		
		
		//obtain starting level for a particular degree
		String beginningLvl = "1";
		//if postgraduate entry, then starting level would be 4th level
		if(degree.getCode().charAt(3) == 'P') beginningLvl = "4";

		
		try (Connection con = DriverManager.getConnection(sqldriver.getDB(), sqldriver.getDBuser(), sqldriver.getDBpassword())) {
			if(email.equals(""))
			{
					// check number of people with same surname and first letter of forname
	
					String query = "SELECT COUNT(*) FROM Student WHERE forname LIKE ? AND surname = ?";
					PreparedStatement pst1 = con.prepareStatement(query);
					pst1.setString(1,  forname.substring(0, 1)+"%");
					pst1.setString(2, surname);
					ResultSet rs = pst1.executeQuery();
					rs.next();
					int numOfRows = rs.getInt(1);
		            System.out.print(numOfRows);
		            numOfRows += 1;
		            
		            email = forname.substring(0, 1) + surname + String.valueOf(numOfRows) +  universityDomain;
		            student.setEmail(email);
			}
			
			//check if username is already assigned to student
			String query = "SELECT * FROM Student WHERE username = ?";
			PreparedStatement pst3 = con.prepareStatement(query);
			pst3.setString(1, username);
			ResultSet rs = pst3.executeQuery();
			if (rs.next()) {
				infoBox("Student with given username already exist.", "Warning");
				con.close();
				return false;
			}
			
			//add student
			String insertStuQ = "INSERT INTO Student (codeOfDegree, username, title, surname, forname, email, personalTutor)" + "VALUES (?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement pst2 = con.prepareStatement(insertStuQ);
			pst2.setString(1, degree.getCode());
			pst2.setString(2, username);
			pst2.setString(3, title);
			pst2.setString(4, surname);
			pst2.setString(5, forname);
			pst2.setString(6, email);
			pst2.setString(7, personalTutor);
			pst2.executeUpdate();
			
			//get the registration number assigned to student
			String stuq = "SELECT registrationNum FROM Student WHERE username = ?";
			PreparedStatement pst4 = con.prepareStatement(query);
			pst4.setString(1, username);
			ResultSet rs2 = pst4.executeQuery();
			if (rs2.next())
				registrationNum = rs2.getInt(1);

			student.setRegistrationID(registrationNum);
			
			//register for initial period
			String insertStuPerQ = "INSERT INTO StudentStudyPeriod (registrationNum, label, level)" + "VALUES (?, ?, ?)";
			PreparedStatement pst5 = con.prepareStatement(insertStuPerQ);
			pst5.setInt(1, registrationNum);
			pst5.setString(2, periodOfStudy);
			pst5.setString(2, periodOfStudy);
			pst5.setString(3, beginningLvl); // to change (if masters then lvl 4)
			pst5.executeUpdate();
			
			con.close();
			
			//add core modules
			Module[] coreModules = getCoreModules(degree,beginningLvl);
			
			for(Module m : coreModules)
			{
				if(!moduleRegister(m, student))
				{
					infoBox("Couldn't register student to core modules.", "Warning");
					return false;
				}
			}
			
			return true;
			
		} catch (Exception exc) {
			//warning message
			infoBox("Student could not be added.", "Warning");
			exc.printStackTrace();
			return false;
		}

	}
	
	/**
	* A class designed to get Core modules from database
	*/
	Module[] getCoreModules(Degree degree, String level)
	{
		try (Connection con = DriverManager.getConnection(sqldriver.getDB(), sqldriver.getDBuser(), sqldriver.getDBpassword())) {
		  
			PreparedStatement pst1 = con.prepareStatement("SELECT * FROM ModuleDegree WHERE codeOfDegree = ? AND isCore = '1' AND level = ? ;");
			pst1.setString(1, degree.getCode());
			pst1.setString(2, level);
			ResultSet rs = pst1.executeQuery();
			
			int nCol = rs.getMetaData().getColumnCount();
			List<Module> modules = new ArrayList<>();
			
			while( rs.next()) {
			    Module m = new Module(rs.getString(1), "", 0);
			    m.completeFromDB();
			    modules.add( m );
			}
			
			Module[] arr = modules.toArray(new Module[modules.size()]);
		
			
		con.close();
		return arr;
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}
	
	public boolean moduleRegister(Module module, Student student) {
		
		try (Connection con = DriverManager.getConnection(sqldriver.getDB(), sqldriver.getDBuser(), sqldriver.getDBpassword())) {
			
			//register for initial period
			String insertStuPerQ = "INSERT INTO ModuleRegistration (registrationNum, codeOfModule)" + "VALUES (?, ?)";
			PreparedStatement pst = con.prepareStatement(insertStuPerQ);
			pst.setInt(1, student.getRegistrationID());
			pst.setString(2, module.getCodeOfModule());
			pst.executeUpdate();
			
			
			con.close();
			return true;
		} catch (Exception exc) {
			//warning message
			infoBox("Module could not be registered.", "Warning");
			exc.printStackTrace();
			return false;
		}
	}
	/**
	*A class designed to add and drop optional modules
	*/
	public boolean addDropOptionalModules(List<Module> addModules, List<Module> dropModules, Student student)
	{
			try (Connection con = DriverManager.getConnection(sqldriver.getDB(), sqldriver.getDBuser(), sqldriver.getDBpassword())) {
				
				System.out.println("number of add modules: " + addModules.size());
				
				//add modules
				for(Module module : addModules){
					String insertMod= "INSERT INTO ModuleRegistration (codeOfModule, registrationNum) " +
										   "SELECT ?, ? "+
										   "FROM dual "+
										   "WHERE NOT EXISTS (SELECT * "+
										                      "FROM ModuleRegistration "+
										                      "WHERE codeOfModule = ? "+
										                      "AND registrationNum = ?); ";
					PreparedStatement pst = con.prepareStatement(insertMod);
					pst.setString(1, module.getCodeOfModule());
					pst.setInt(2, student.getRegistrationID());
					pst.setString(3, module.getCodeOfModule());
					pst.setInt(4, student.getRegistrationID());
					pst.executeUpdate();
					
					System.out.println("added module: " + module.getCodeOfModule());
				}
				
				
				System.out.println("number of drop modules: " + dropModules.size());
				//drop modules
				for(Module module : dropModules){
					String insertMod= "DELETE FROM ModuleRegistration WHERE codeOfModule = ? AND registrationNum = ?";
					PreparedStatement pst = con.prepareStatement(insertMod);
					pst.setString(1, module.getCodeOfModule());
					pst.setInt(2, student.getRegistrationID());
					pst.executeUpdate();
					
					System.out.println("droped module: " + module.getCodeOfModule());
				}
				
				con.close();
				return true;
			} catch (Exception exc) {
				//warning message
				infoBox("Optional modules could not been modified", "Warning");
				exc.printStackTrace();
				return false;
			}

	}
	
	//registration check
	public boolean checkRegistration() {
		return false;
	}
	
	//Credits check
	public boolean checkCredits() {
		return false;
	}

}
