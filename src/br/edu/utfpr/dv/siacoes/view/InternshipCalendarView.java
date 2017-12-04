package br.edu.utfpr.dv.siacoes.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.renderers.DateRenderer;

import br.edu.utfpr.dv.siacoes.Session;
import br.edu.utfpr.dv.siacoes.bo.CertificateBO;
import br.edu.utfpr.dv.siacoes.bo.InternshipBO;
import br.edu.utfpr.dv.siacoes.bo.InternshipJuryAppraiserBO;
import br.edu.utfpr.dv.siacoes.bo.InternshipJuryBO;
import br.edu.utfpr.dv.siacoes.bo.InternshipJuryStudentBO;
import br.edu.utfpr.dv.siacoes.bo.SemesterBO;
import br.edu.utfpr.dv.siacoes.components.SemesterComboBox;
import br.edu.utfpr.dv.siacoes.components.YearField;
import br.edu.utfpr.dv.siacoes.model.CalendarReport;
import br.edu.utfpr.dv.siacoes.model.Internship;
import br.edu.utfpr.dv.siacoes.model.InternshipJury;
import br.edu.utfpr.dv.siacoes.model.InternshipJuryAppraiser;
import br.edu.utfpr.dv.siacoes.model.InternshipJuryFormReport;
import br.edu.utfpr.dv.siacoes.model.InternshipJuryStudent;
import br.edu.utfpr.dv.siacoes.model.JuryFormReport;
import br.edu.utfpr.dv.siacoes.model.Semester;
import br.edu.utfpr.dv.siacoes.model.Module.SystemModule;
import br.edu.utfpr.dv.siacoes.util.DateUtils;
import br.edu.utfpr.dv.siacoes.util.ReportUtils;
import br.edu.utfpr.dv.siacoes.window.EditInternshipJuryAppraiserFeedbackWindow;
import br.edu.utfpr.dv.siacoes.window.EditInternshipJuryWindow;

public class InternshipCalendarView extends ListView {

	public static final String NAME = "internshipcalendar";
	
	private final SemesterComboBox comboSemester;
	private final YearField textYear;
	private final Button buttonFile;
	private final Button buttonForm;
	private final Button buttonCalendar;
	private final Button buttonStatements;
	private final Button buttonSingleStatement;
	private final Button buttonSendFeedback;
	private final Button buttonParticipants;
	
	private boolean listAll = false;
	
	public InternshipCalendarView(){
		super(SystemModule.SIGES);
		
		this.setCaption("Agenda de Bancas de Est�gio");
		
		Semester semester;
		try {
			semester = new SemesterBO().findByDate(Session.getUser().getDepartment().getCampus().getIdCampus(), DateUtils.getToday().getTime());
		} catch (Exception e) {
			semester = new Semester();
		}
		
		this.comboSemester = new SemesterComboBox();
		this.comboSemester.select(semester.getSemester());
		
		this.textYear = new YearField();
		this.textYear.setYear(semester.getYear());
		
		this.addFilterField(new HorizontalLayout(this.comboSemester, this.textYear));
		
		this.setAddVisible(false);
		this.setEditVisible(false);
		this.setDeleteVisible(false);
		
		this.buttonFile = new Button("Down. do Relat�rio", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadFile();
            }
        });
		
		this.buttonForm = new Button("Ficha de Avalia��o", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadForm();
            }
        });
		
		this.buttonParticipants = new Button("Lista de Presen�a", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadParticipants();
            }
        });
		
		this.buttonSendFeedback = new Button("Feedback", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	sendFeedbackClick();
            }
        });
		
		this.buttonStatements = new Button("Declara��es", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadStatements();
            }
        });
		
		this.buttonSingleStatement = new Button("Declara��o", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	downloadSingleStatement();
            }
        });
		
		this.buttonCalendar = new Button("Imprimir");
		
		this.addActionButton(this.buttonCalendar);
		this.addActionButton(this.buttonFile);
		this.addActionButton(this.buttonForm);
		this.addActionButton(this.buttonParticipants);
		this.addActionButton(this.buttonSendFeedback);
		this.addActionButton(this.buttonSingleStatement);
		
		if(Session.isUserManager(this.getModule())){
			this.addActionButton(this.buttonStatements);
			this.setEditVisible(true);
			this.setEditCaption("Banca");
		}
	}
	
	private void configureButtons(){
		if(this.listAll){
			this.buttonSendFeedback.setVisible(false);
			this.buttonForm.setVisible(Session.isUserManager(this.getModule()));
			this.buttonParticipants.setVisible(Session.isUserManager(this.getModule()));
			this.buttonSingleStatement.setVisible(false);
			this.buttonFile.setVisible(Session.isUserManager(this.getModule()));
		}else{
			this.buttonSendFeedback.setVisible(true);
			this.buttonFile.setVisible(Session.isUserProfessor());
			this.buttonForm.setVisible(Session.isUserProfessor());
			this.buttonParticipants.setVisible(Session.isUserProfessor());
			this.buttonSendFeedback.setVisible(Session.isUserProfessor());
			this.buttonStatements.setVisible(false);
			this.buttonCalendar.setVisible(Session.isUserProfessor());
		}
	}
	
	@Override
	protected void loadGrid() {
		this.getGrid().addColumn("Data e Hora", Date.class).setRenderer(new DateRenderer(new SimpleDateFormat("dd/MM/yyyy HH:mm")));
		this.getGrid().addColumn("Local", String.class);
		this.getGrid().addColumn("Acad�mico", String.class);
		this.getGrid().addColumn("Empresa", String.class);
		this.getGrid().getColumns().get(0).setWidth(165);
		
		try {
			InternshipJuryBO bo = new InternshipJuryBO();
			List<InternshipJury> list;
			List<CalendarReport> report = null;
			
			if(this.listAll){
				list = bo.listBySemester(Session.getUser().getDepartment().getIdDepartment(), this.comboSemester.getSemester(), this.textYear.getYear());
				report = bo.getCalendarReport(Session.getUser().getDepartment().getIdDepartment(), 0, this.comboSemester.getSemester(), this.textYear.getYear());
			}else{
				if(Session.isUserProfessor()){
					list = bo.listByAppraiser(Session.getUser().getIdUser(), this.comboSemester.getSemester(), this.textYear.getYear());
					report = bo.getCalendarReport(Session.getUser().getDepartment().getIdDepartment(), Session.getUser().getIdUser(), this.comboSemester.getSemester(), this.textYear.getYear());
				}else{
					list = bo.listByStudent(Session.getUser().getIdUser(), this.comboSemester.getSemester(), this.textYear.getYear());
				}
			}
			
			if(this.listAll || Session.isUserProfessor()){
				new ReportUtils().prepareForPdfReport("InternshipCalendar", "Agenda de Defesas", report, this.buttonCalendar);
			}
			
	    	for(InternshipJury jury : list){
	    		InternshipBO ibo = new InternshipBO();
	    		Internship internship = ibo.findById(jury.getInternship().getIdInternship());
	    		
				Object itemId = this.getGrid().addRow(jury.getDate(), jury.getLocal(), internship.getStudent().getName(), internship.getCompany().getName());
				this.addRowId(itemId, jury.getIdInternshipJury());
			}
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
			
			Notification.show("Listar Bancas", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}
	
	private void downloadFile(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Download do Trabalho", "Selecione uma banca para fazer o download do trabalho.", Notification.Type.WARNING_MESSAGE);
		}else{
			try {
				InternshipJuryBO bo = new InternshipJuryBO();
				InternshipJury jury = bo.findById((int)value);
				
				InternshipBO ibo = new InternshipBO();
				Internship internship = ibo.findById(jury.getInternship().getIdInternship());
				
				if(internship.getFinalReport() == null){
					Notification.show("Download do Trabalho", "O relat�rio final do est�gio n�o foi anexado ao cadastro.", Notification.Type.WARNING_MESSAGE);
				}else{
					this.showReport(internship.getFinalReport());
				}
        	} catch (Exception e) {
        		Notification.show("Download do Trabalho", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void downloadForm(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Imprimir Ficha de Avalia��o", "Selecione uma banca para gerar a ficha de avalia��o.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				InternshipJuryBO bo = new InternshipJuryBO();
				InternshipJuryFormReport report = bo.getFormReport((int)value);
				
				List<InternshipJuryFormReport> list = new ArrayList<InternshipJuryFormReport>();
				list.add(report);

				this.showReport(new ReportUtils().createPdfStream(list, "InternshipJuryForm").toByteArray());
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Imprimir Ficha de Avalia��o", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void downloadParticipants(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Imprimir Lista de Presen�a", "Selecione uma banca para gerar a lista de presen�a.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				InternshipJuryBO bo = new InternshipJuryBO();
				InternshipJuryFormReport report = bo.getFormReport((int)value);
				
				JuryFormReport report2 = new JuryFormReport();
				
				report2.setTitle(report.getCompany());
				report2.setStage(0);
				report2.setStudent(report.getStudent());
				report2.setDate(report.getDate());
				
				List<JuryFormReport> list = new ArrayList<JuryFormReport>();
				list.add(report2);
				
				this.showReport(new ReportUtils().createPdfStream(list, "JuryParticipants").toByteArray());
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Imprimir Lista de Presen�a", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void sendFeedbackClick(){
		Object id = getIdSelected();
    	
    	if(id == null){
    		Notification.show("Selecionar Registro", "Selecione o registro para enviar o feedback.", Notification.Type.WARNING_MESSAGE);
    	}else{
    		try {
    			InternshipJuryAppraiserBO bo = new InternshipJuryAppraiserBO();
    			
    			InternshipJuryAppraiser appraiser = bo.findByAppraiser((int)id, Session.getUser().getIdUser());
    			
    			if(appraiser != null){
    				UI.getCurrent().addWindow(new EditInternshipJuryAppraiserFeedbackWindow(appraiser));
    			}else{
    				Notification.show("Enviar Feedback", "� necess�rio ser membro da banca para enviar o feedback.", Notification.Type.WARNING_MESSAGE);
    			}
			} catch (Exception e) {
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
				
				Notification.show("Enviar Feedback", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
    	}
	}
	
	private void downloadSingleStatement(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Gerar Declara��o", "Selecione uma banca para gerar a declara��o.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				CertificateBO bo = new CertificateBO();
				byte[] report;
				
				if(Session.isUserProfessor()){
					InternshipJuryAppraiserBO jbo = new InternshipJuryAppraiserBO();
					InternshipJuryAppraiser appraiser = jbo.findByAppraiser((int)value, Session.getUser().getIdUser());
					
					report = bo.getInternshipJuryProfessorStatement(appraiser);
				}else{
					InternshipJuryStudentBO jbo = new InternshipJuryStudentBO();
					InternshipJuryStudent student = jbo.findByStudent((int)value, Session.getUser().getIdUser());
					
					report = bo.getInternshipJuryStudentStatement(student);
				}
				
				if(report != null){
					this.showReport(report);
				}else{
					Notification.show("Gerar Declara��o", "N�o foi encontrada a declara��o para imprimir.", Notification.Type.WARNING_MESSAGE);
				}
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Gerar Declara��o", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	private void downloadStatements(){
		Object value = getIdSelected();
		
		if(value == null){
			Notification.show("Gerar Declara��es", "Selecione uma banca para gerar as declara��es.", Notification.Type.WARNING_MESSAGE);
		}else{
			try{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				PDFMergerUtility pdfMerge = new PDFMergerUtility();
				pdfMerge.setDestinationStream(output);
				
				CertificateBO bo = new CertificateBO();

				byte[] reportProfessor = bo.getInternshipJuryProfessorStatementReportList((int)value);
				
				if(reportProfessor != null){
					pdfMerge.addSource(new ByteArrayInputStream(reportProfessor));
				}
				
				byte[] reportStudent = bo.getInternshipJuryStudentStatementReportList((int)value);
				
				if(reportStudent != null){
					pdfMerge.addSource(new ByteArrayInputStream(reportStudent));
				}
				
				if((reportProfessor != null) || (reportStudent != null)){
					pdfMerge.mergeDocuments(null);
					
					byte[] report = output.toByteArray();
					
					this.showReport(report);
				}else{
					Notification.show("Gerar Declara��es", "N�o h� declara��es para serem geradas.", Notification.Type.WARNING_MESSAGE);
				}
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	Notification.show("Gerar Declara��es", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}
	
	@Override
	public void addClick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void editClick(Object id) {
		try {
			InternshipJuryBO bo = new InternshipJuryBO();
			InternshipJury jury = bo.findById((int)id);
			
			UI.getCurrent().addWindow(new EditInternshipJuryWindow(jury, this));
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
			
			Notification.show("Marcar Banca", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}

	@Override
	public void deleteClick(Object id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void filterClick() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enter(ViewChangeEvent event){
		if((event.getParameters() != null) && (!event.getParameters().isEmpty())){
			try{
				int i = Integer.parseInt(event.getParameters().trim());
				
				this.listAll = (i == 1);
			}catch(Exception e){
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
				
				this.listAll = false;
			}
		}
		
		this.configureButtons();
		
		super.enter(event);
	}

}
