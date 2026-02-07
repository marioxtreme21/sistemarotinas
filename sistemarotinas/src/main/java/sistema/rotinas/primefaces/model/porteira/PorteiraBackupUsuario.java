// FILE: src/main/java/sistema/rotinas/primefaces/model/porteira/PorteiraBackupUsuario.java
package sistema.rotinas.primefaces.model.porteira;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "porteira_backup_usuario", indexes = { @Index(name = "idx_pbu_backup", columnList = "backup_id"),
		@Index(name = "idx_pbu_user", columnList = "user"), @Index(name = "idx_pbu_name", columnList = "name") })
public class PorteiraBackupUsuario implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Back-up "pai"
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "backup_id", nullable = false)
	private PorteiraBackup backup;

	// =========================
	// Campos "espelho" do payload
	// =========================
	@Column(name = "api_id", length = 50)
	private String apiId; // "id" do JSON da porteira (não é o ID do banco)

	@Column(name = "name", length = 255)
	private String name;

	@Column(name = "user", length = 255)
	private String user;

	@Column(name = "password", length = 255)
	private String password;

	@Column(name = "card", length = 255)
	private String card;

	@Column(name = "qrcode", length = 255)
	private String qrcode;

	@Column(name = "rfcode", length = 255)
	private String rfcode;

	@Column(name = "fingerprint", length = 2048)
	private String fingerprint;

	@Column(name = "validity", length = 50)
	private String validity;

	@Column(name = "lifecount", length = 50)
	private String lifecount;

	@Column(name = "accessibility")
	private Boolean accessibility;

	@Column(name = "panic")
	private Boolean panic;

	@Column(name = "key_user", length = 255)
	private String keyUser; // "key"

	@Column(name = "user_interface", length = 255)
	private String userInterface; // "interface"

	@Column(name = "administrator")
	private Boolean administrator;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "apn", length = 255)
	private String apn;

	@Column(name = "fcm", length = 255)
	private String fcm;

	@Column(name = "visitor")
	private Boolean visitor;

	@Column(name = "relay", length = 50)
	private String relay;

	@Column(name = "finger", length = 255)
	private String finger;

	@Column(name = "face", length = 255)
	private String face;

	// ✅ JSON completo do usuário (todas as chaves)
	@Lob
	@Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
	private String payloadJson;

	@Column(name = "data_backup", nullable = false)
	private LocalDateTime dataBackup;

	@PrePersist
	public void prePersist() {
		if (dataBackup == null)
			dataBackup = LocalDateTime.now();
		if (payloadJson == null)
			payloadJson = "{}";
	}

	// =========================
	// Getters/Setters
	// =========================
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PorteiraBackup getBackup() {
		return backup;
	}

	public void setBackup(PorteiraBackup backup) {
		this.backup = backup;
	}

	public String getApiId() {
		return apiId;
	}

	public void setApiId(String apiId) {
		this.apiId = apiId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCard() {
		return card;
	}

	public void setCard(String card) {
		this.card = card;
	}

	public String getQrcode() {
		return qrcode;
	}

	public void setQrcode(String qrcode) {
		this.qrcode = qrcode;
	}

	public String getRfcode() {
		return rfcode;
	}

	public void setRfcode(String rfcode) {
		this.rfcode = rfcode;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	public String getValidity() {
		return validity;
	}

	public void setValidity(String validity) {
		this.validity = validity;
	}

	public String getLifecount() {
		return lifecount;
	}

	public void setLifecount(String lifecount) {
		this.lifecount = lifecount;
	}

	public Boolean getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(Boolean accessibility) {
		this.accessibility = accessibility;
	}

	public Boolean getPanic() {
		return panic;
	}

	public void setPanic(Boolean panic) {
		this.panic = panic;
	}

	public String getKeyUser() {
		return keyUser;
	}

	public void setKeyUser(String keyUser) {
		this.keyUser = keyUser;
	}

	public String getUserInterface() {
		return userInterface;
	}

	public void setUserInterface(String userInterface) {
		this.userInterface = userInterface;
	}

	public Boolean getAdministrator() {
		return administrator;
	}

	public void setAdministrator(Boolean administrator) {
		this.administrator = administrator;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getApn() {
		return apn;
	}

	public void setApn(String apn) {
		this.apn = apn;
	}

	public String getFcm() {
		return fcm;
	}

	public void setFcm(String fcm) {
		this.fcm = fcm;
	}

	public Boolean getVisitor() {
		return visitor;
	}

	public void setVisitor(Boolean visitor) {
		this.visitor = visitor;
	}

	public String getRelay() {
		return relay;
	}

	public void setRelay(String relay) {
		this.relay = relay;
	}

	public String getFinger() {
		return finger;
	}

	public void setFinger(String finger) {
		this.finger = finger;
	}

	public String getFace() {
		return face;
	}

	public void setFace(String face) {
		this.face = face;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public LocalDateTime getDataBackup() {
		return dataBackup;
	}

	public void setDataBackup(LocalDateTime dataBackup) {
		this.dataBackup = dataBackup;
	}
}