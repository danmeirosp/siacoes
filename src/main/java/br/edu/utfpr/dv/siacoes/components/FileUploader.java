package br.edu.utfpr.dv.siacoes.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.verapdf.core.VeraPDFException;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.results.ValidationResult;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

import br.edu.utfpr.dv.siacoes.model.Document.DocumentType;

public class FileUploader extends HorizontalLayout {

	private final Upload uploadFile;
	private final Image imageFileUploaded;
	private final ProgressBar progressBar;
	private final List<DocumentType> acceptedDocumentTypes;
	private long maxBytesLength;
	private FileUploaderListener fileUploadListener;
	private byte[] uploadedFile;
	private DocumentType fileType;
	private boolean validatePDFAFile;
	
	public FileUploader(String caption) {
		DocumentUploader listener = new DocumentUploader();
		
		this.uploadFile = new Upload(caption, listener);
		this.uploadFile.addSucceededListener(listener);
		this.uploadFile.addStartedListener(listener);
		this.uploadFile.addProgressListener(listener);
		this.setButtonCaption("Enviar Arquivo");
		this.uploadFile.setImmediate(true);
		
		this.imageFileUploaded = new Image("", new ThemeResource("images/ok.png"));
		this.imageFileUploaded.setVisible(false);
		
		this.progressBar = new ProgressBar();
		this.progressBar.setWidth("50px");
		this.progressBar.setVisible(false);
		
		this.addComponent(this.uploadFile);
		this.addComponent(this.progressBar);
		this.addComponent(this.imageFileUploaded);
		
		this.setComponentAlignment(this.uploadFile, Alignment.MIDDLE_LEFT);
		this.setComponentAlignment(this.progressBar, Alignment.MIDDLE_CENTER);
		this.setComponentAlignment(this.imageFileUploaded, Alignment.MIDDLE_RIGHT);
		
		this.acceptedDocumentTypes = new ArrayList<DocumentType>();
		this.setMaxBytesLength(0);
	}
	
	public void setCaption(String caption) {
		this.uploadFile.setCaption(caption);
	}
	
	public String getCaption() {
		return this.uploadFile.getCaption();
	}
	
	public FileUploaderListener getFileUploadListener() {
		return fileUploadListener;
	}

	public void setFileUploadListener(FileUploaderListener fileUploadListener) {
		this.fileUploadListener = fileUploadListener;
	}

	public byte[] getUploadedFile() {
		return uploadedFile;
	}

	public DocumentType getFileType() {
		return fileType;
	}

	public long getMaxBytesLength() {
		return maxBytesLength;
	}

	public void setMaxBytesLength(long maxBytesLength) {
		this.maxBytesLength = maxBytesLength;
	}

	public List<DocumentType> getAcceptedDocumentTypes() {
		return acceptedDocumentTypes;
	}
	
	public void setButtonCaption(String buttonCaption) {
		this.uploadFile.setButtonCaption(buttonCaption);
	}
	
	public String getButtonCaption() {
		return this.uploadFile.getButtonCaption();
	}
	
	private String getStringMaxBytesLength() {
		double value = (double)this.getMaxBytesLength();
		String[] units = {"bytes", "KB", "MB", "GB", "TB", "PB", "YB"};
		int index = 0;
		
		while(value >= 1024) {
			value = value / 1024;
			index++;
		}
		
		return String.format("%.2f", value) + " " + units[index];
	}
	
	private boolean isDocumentTypeAccept(DocumentType docType) {
		this.validatePDFAFile = false;
		
		if((this.getAcceptedDocumentTypes() == null) || (this.getAcceptedDocumentTypes().size() == 0)) {
			return true;
		}
		
		for(DocumentType doc : this.getAcceptedDocumentTypes()) {
			if(doc == docType) {
				return true;
			}
		}
		
		if(docType == DocumentType.PDF) {
			for(DocumentType doc : this.getAcceptedDocumentTypes()) {
				if(doc == DocumentType.PDFA) {
					this.validatePDFAFile = true;
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean validatePDFA(byte[] file) {
		String tempName = UUID.randomUUID().toString() + ".pdf";
		
		Path path = Paths.get(tempName);
		
		this.validatePDFAFile = false;
		
		try {
			Files.write(path, file);
			
			VeraGreenfieldFoundryProvider.initialise();
			PDFAParser parser = Foundries.defaultInstance().createParser(path.toFile());
			PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
			ValidationResult result = validator.validate(parser);
		    return result.isCompliant();
		} catch (IOException e1) {
			Logger.getGlobal().log(Level.SEVERE, e1.getMessage(), e1);
			
			return false;
		} catch (VeraPDFException e2) {
			Logger.getGlobal().log(Level.SEVERE, e2.getMessage(), e2);
			
			return false;
		} catch (NoSuchElementException e3) {
			Logger.getGlobal().log(Level.SEVERE, e3.getMessage(), e3);
			
			return false;
		} finally {
			try {
				Files.delete(path);
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	private String getStringAcceptedDocumentTypes() {
		if((this.getAcceptedDocumentTypes() == null) || (this.getAcceptedDocumentTypes().size() == 0)) {
			return "(Todos os documentos)";
		}
		
		String ret = this.getAcceptedDocumentTypes().get(0).toString();
		
		for(int i = 1; i < this.getAcceptedDocumentTypes().size(); i++) {
			ret = ret + ", " + this.getAcceptedDocumentTypes().get(i).toString();
		}
		
		return ret;
	}
	
	private void setSuccess() {
		this.progressBar.setVisible(false);
		this.imageFileUploaded.setSource(new ThemeResource("images/ok.png"));
		this.imageFileUploaded.setVisible(true);
		this.imageFileUploaded.setDescription(null);
	}
	
	private void setError(String errorMessage) {
		this.progressBar.setVisible(false);
		this.imageFileUploaded.setSource(new ThemeResource("images/error.png"));
		this.imageFileUploaded.setVisible(true);
		this.imageFileUploaded.setDescription(errorMessage);
	}
	
	private void setProgress() {
		this.progressBar.setValue(0.0f);
		this.progressBar.setVisible(true);
		this.imageFileUploaded.setVisible(false);
	}

	@SuppressWarnings("serial")
	class DocumentUploader implements Receiver, SucceededListener, StartedListener, ProgressListener {
		private File tempFile = null;
		
		@Override
		public OutputStream receiveUpload(String filename, String mimeType) {
			try {
				if(!isDocumentTypeAccept(DocumentType.fromMimeType(mimeType))) {
					throw new Exception("O arquivo precisa estar em um dos seguintes formatos: " + getStringAcceptedDocumentTypes() + ".");
				}

				fileType = DocumentType.fromMimeType(mimeType);
	            tempFile = File.createTempFile(filename, "tmp");
	            tempFile.deleteOnExit();
	            return new FileOutputStream(tempFile);
	        } catch (Exception e) {
	        	Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	setError(e.getMessage());
	            
	            Notification.show("Carregamento do Arquivo", e.getMessage(), Notification.Type.ERROR_MESSAGE);
	        }

	        return null;
		}
		
		@Override
		public void uploadSucceeded(SucceededEvent event) {
			try {
	            FileInputStream input = new FileInputStream(tempFile);
	            
	            if((getMaxBytesLength() > 0) && (input.available() > getMaxBytesLength())) {
	            	throw new Exception("O arquivo precisa ter um tamanho máximo de " + getStringMaxBytesLength() + ".");
	            }
	            
	            byte[] buffer = new byte[input.available()];
	            
	            input.read(buffer);
	            
	            if(validatePDFAFile && !validatePDFA(buffer)) {
	            	throw new Exception("O arquivo precisa estar no formato PDF/A-1b (padrão utilizado para arquivamento de longo prazo de documentos eletrônicos).");
	            }
	            
	            uploadedFile = buffer;
	            
	            setSuccess();
	            
	            fileUploadListener.uploadSucceeded();
	            
	            Notification.show("Carregamento do Arquivo", "O arquivo foi enviado com sucesso.\n\nClique em SALVAR para concluir a submissão.", Notification.Type.HUMANIZED_MESSAGE);
	        } catch (Exception e) {
	        	Logger.getGlobal().log(Level.SEVERE, e.getMessage(), e);
	        	
	        	setError(e.getMessage());
	            
	            Notification.show("Carregamento do Arquivo", e.getMessage(), Notification.Type.ERROR_MESSAGE);
	        }
		}

		@Override
		public void updateProgress(long readBytes, long contentLength) {
			progressBar.setValue((float)readBytes / contentLength);
		}

		@Override
		public void uploadStarted(StartedEvent event) {
			setProgress();
		}
	}
	
}