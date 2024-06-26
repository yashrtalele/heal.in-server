package in.app.heal.service;

import in.app.heal.aux.AuxChangePasswordDTO;
import in.app.heal.aux.AuxForgotPasswordDTO;
import in.app.heal.aux.AuxUserDTO;
import in.app.heal.aux.LoginDTO;
import in.app.heal.entities.User;
import in.app.heal.entities.UserCredentials;
import in.app.heal.error.ApiError;
import in.app.heal.repository.JournalEntryRepository;
import in.app.heal.repository.PublicQNARepository;
import in.app.heal.repository.UserCredentialsRepository;
import in.app.heal.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpHeaders;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class UserCredentialsService {
  @Autowired private TokenService tokenService;
  @Autowired private UserService userService;
  @Autowired private UserCredentialsRepository repository;
  @Autowired private UserRepository userRepository;
  @Autowired private PublicQNARepository publicQNARepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private JavaMailSender mailSender;
  @Autowired private TemplateEngine templateEngine;
  @Autowired private PasswordService passwordService;
  public void addUser(UserCredentials userCredentials) {
    repository.save(userCredentials);
  }
  public Optional<UserCredentials> findByEmail(String email) {
    return Optional.ofNullable(repository.findByEmail(email));
  }

  public ResponseEntity<?> loginUser(LoginDTO loginDTO) {
    if(loginDTO.getEmail()== null || loginDTO.getEmail().isEmpty() || loginDTO.getPassword()== null ||  loginDTO.getPassword().isEmpty()){
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.BAD_REQUEST);
      apiError.setMessage("Missing credentials");
      return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
    Optional<UserCredentials> userCredentials =
        this.findByEmail(loginDTO.getEmail());
    if (userCredentials.isPresent()) {
      UserCredentials userCredentialsfound = userCredentials.get();
      String password = userCredentialsfound.getPassword();
      boolean match = passwordService.checkPassword(loginDTO.getPassword(), password);
      if (match) {
        String jwtToken = tokenService.generateToken(loginDTO.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);
        return new ResponseEntity<>(headers, HttpStatus.OK);
      } else {
        ApiError apiError = new ApiError();
        apiError.setMessage("Incorrect password");
        apiError.setStatus(HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
      }
    }
    ApiError apiError = new ApiError();
    apiError.setMessage("User does not exist");
    apiError.setStatus(HttpStatus.CONFLICT);
    return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
  }
  public ResponseEntity<?> getProfileDetails(String auth) {
    String token = tokenService.getToken(auth);
    if (token.isEmpty()) {
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.UNAUTHORIZED);
      apiError.setMessage("Missing token");
      return new ResponseEntity<>(apiError,HttpStatus.UNAUTHORIZED);
    }
    try {
      String email = tokenService.getEmailFromToken(token);
      Optional<UserCredentials> userCredentialsOptional =
          this.findByEmail(email);
      if (userCredentialsOptional.isPresent()) {
        UserCredentials userCredentials = userCredentialsOptional.get();
        User user = userCredentials.getUser_id();
        AuxUserDTO auxUserDTO = new AuxUserDTO();
        auxUserDTO.setAge(user.getAge());
        auxUserDTO.setGender(user.getGender());
        auxUserDTO.setEmail(email);
        auxUserDTO.setRole(userCredentials.getRole());
        auxUserDTO.setContact(user.getContact_number());
        auxUserDTO.setFirstName(user.getFirst_name());
        auxUserDTO.setLastName(user.getLast_name());
        auxUserDTO.setUserId(user.getUser_id());
        return new ResponseEntity<AuxUserDTO>(auxUserDTO, HttpStatus.OK);
      }
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.CONFLICT);
      apiError.setMessage("User not found");
      return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    } catch (Exception e) {
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      apiError.setMessage(e.getMessage());
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public ResponseEntity<?> deleteProfile(String auth) {
    String token = tokenService.getToken(auth);
    if (token.isEmpty()) {
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.UNAUTHORIZED);
      apiError.setMessage("Missing token");
      return new ResponseEntity<>(apiError,HttpStatus.UNAUTHORIZED);
    }
    try {
      String email = tokenService.getEmailFromToken(token);
      Optional<UserCredentials> userCredentialsOptional =
          this.findByEmail(email);
      if (userCredentialsOptional.isPresent()) {
        UserCredentials credentials = userCredentialsOptional.get();
        User user = credentials.getUser_id();
        userRepository.delete(user);
        return new ResponseEntity<>(HttpStatus.OK);
      }
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.CONFLICT);
      apiError.setMessage("User not found");
      return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }catch (Exception e) {
      System.out.println(e);
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      apiError.setMessage(e.getMessage());
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public ResponseEntity<?> deleteData(String auth) {
    String token = tokenService.getToken(auth);
    if (token.isEmpty()) {
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.UNAUTHORIZED);
      apiError.setMessage("Missing token");
      return new ResponseEntity<>(apiError,HttpStatus.UNAUTHORIZED);
    }
    try {
      String email = tokenService.getEmailFromToken(token);
      Optional<UserCredentials> userCredentialsOptional =
          this.findByEmail(email);
      if (userCredentialsOptional.isPresent()) {
        UserCredentials credentials = userCredentialsOptional.get();
        publicQNARepository.deleteByUserId(credentials.getUser_id().getUser_id());
        journalEntryRepository.deleteByUserId(credentials.getUser_id().getUser_id());
        return new ResponseEntity<>(HttpStatus.OK);
      }
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.CONFLICT);
      apiError.setMessage("User not found");
      return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }catch (Exception e) {
      System.out.println(e);
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      apiError.setMessage(e.getMessage());
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }  
  public ResponseEntity<?> registerUser(AuxUserDTO auxUserDTO) {
    if(auxUserDTO.getFirstName()== null || auxUserDTO.getFirstName().isEmpty() || 
      auxUserDTO.getLastName()== null || auxUserDTO.getLastName().isEmpty() ||
      auxUserDTO.getEmail()== null || auxUserDTO.getEmail().isEmpty() || 
      auxUserDTO.getPassword()== null ||  auxUserDTO.getPassword().isEmpty() || 
      auxUserDTO.getAge()== null || auxUserDTO.getAge().equals(0) ||
      auxUserDTO.getContact()== null || auxUserDTO.getContact().equals(0) ||
      auxUserDTO.getGender()== null || auxUserDTO.getEmail().isEmpty() 
      ){
      ApiError apiError = new ApiError();
      apiError.setStatus(HttpStatus.BAD_REQUEST);
      apiError.setMessage("Missing credentials");
      return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
    Optional<UserCredentials> alreadyExisting =
        this.findByEmail(auxUserDTO.getEmail());
    if (alreadyExisting.isPresent()) {
      System.out.println("User already exists");
      ApiError apiError = new ApiError();
      apiError.setMessage("User already exists");
      apiError.setStatus(HttpStatus.CONFLICT);
      return new ResponseEntity<Object>(apiError, HttpStatus.CONFLICT);
    }
    User user = userService.populateUser(auxUserDTO);
    UserCredentials newUserCredentials = new UserCredentials();
    newUserCredentials.setEmail(auxUserDTO.getEmail());
    newUserCredentials.setUser_id(user);
    String hash = passwordService.hashPassword(auxUserDTO.getPassword());
    newUserCredentials.setPassword(hash);
    newUserCredentials.setRole(auxUserDTO.getRole());
    this.addUser(newUserCredentials);
    String jwtToken = tokenService.generateToken(auxUserDTO.getEmail());
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwtToken);
    return new ResponseEntity<>(headers, HttpStatus.OK);
  }

  public void sendEmail(String email, String subject, String message) {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
    try {
      helper.setTo(email);
      helper.setSubject(subject);
      Context context = new Context();
      context.setVariable("message", message);
      String htmlContent =
          templateEngine.process("email_otp_template.html", context);
      helper.setText(htmlContent, true);
      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  public ResponseEntity<?>
  forgotPassword(AuxForgotPasswordDTO forgotPasswordDTO) {
    Optional<UserCredentials> alreadyExisting =
        this.findByEmail(forgotPasswordDTO.getEmail());
    if (alreadyExisting.isEmpty()) {
      System.out.println("User does not exist");
      ApiError apiError = new ApiError();
      apiError.setMessage("User does not exist");
      apiError.setStatus(HttpStatus.CONFLICT);
      return new ResponseEntity<Object>(apiError, HttpStatus.CONFLICT);
    }
    Random random = new Random();
    int otp = 1000 + random.nextInt(9000);
    String email = forgotPasswordDTO.getEmail();
    String subject = "Forgot Password";
    String message = "" + otp;
    sendEmail(email, subject, message);
    return new ResponseEntity<Integer>(otp, HttpStatus.OK);
  }

  public ResponseEntity<?>
  changePassword(AuxChangePasswordDTO changePasswordDTO) {
    Optional<UserCredentials> alreadyExisting =
        this.findByEmail(changePasswordDTO.getEmail());
    if (alreadyExisting.isEmpty()) {
      System.out.println("User does not exist");
      ApiError apiError = new ApiError();
      apiError.setMessage("User does not exist");
      apiError.setStatus(HttpStatus.CONFLICT);
      return new ResponseEntity<Object>(apiError, HttpStatus.CONFLICT);
    }
    String hash = passwordService.hashPassword(changePasswordDTO.getPassword());
    alreadyExisting.get().setPassword(hash);
    this.addUser(alreadyExisting.get());
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  public ResponseEntity<?>
  updatePassword(AuxChangePasswordDTO changePasswordDTO) {
    Optional<UserCredentials> alreadyExisting =
        this.findByEmail(changePasswordDTO.getEmail());
    if (alreadyExisting.isPresent()) {
      UserCredentials userCredentialsfound = alreadyExisting.get();
      String password = userCredentialsfound.getPassword();
      boolean match = passwordService.checkPassword(changePasswordDTO.getCurrentPassword(), password);
      if (match) {
        String hash = passwordService.hashPassword(changePasswordDTO.getPassword());
        alreadyExisting.get().setPassword(hash);
        this.addUser(alreadyExisting.get());
        return new ResponseEntity<String>(HttpStatus.OK);
      } else {
        ApiError apiError = new ApiError();
        apiError.setMessage("Incorrect password");
        apiError.setStatus(HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
      }
    }
    ApiError apiError = new ApiError();
    apiError.setMessage("User does not exist");
    apiError.setStatus(HttpStatus.CONFLICT);
    return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
  }
}
