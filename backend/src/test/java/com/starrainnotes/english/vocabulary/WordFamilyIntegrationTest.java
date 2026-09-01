package com.starrainnotes.english.vocabulary;
import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WordFamilyIntegrationTest extends AbstractAuthIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
 @BeforeEach void setup(){jdbc.update("DELETE FROM admin_user");jdbc.update("INSERT INTO admin_user(username,password_hash) VALUES('family-admin',?)",encoder.encode("family-pass-123"));}
 @AfterEach void cleanup(){jdbc.update("DELETE FROM vocabulary_word_family WHERE slug='test-family-act'");jdbc.update("DELETE FROM admin_user");}
 @Test void managesMembersAndServesPublicFamily()throws Exception{
  MockHttpSession session=login();
  MvcResult created=mockMvc.perform(withCsrf(post("/api/v1/admin/english/vocabulary/families").session(session).contentType("application/json").content("{\"headWord\":\"act\",\"slug\":\"test-family-act\",\"description\":\"action family\",\"wordIds\":[]}"),fetchCsrfToken())).andExpect(status().isCreated()).andReturn();
  String body=created.getResponse().getContentAsString();long id=Long.parseLong(body.replaceFirst("(?s).*\\\"id\\\":(\\d+).*","$1"));
  mockMvc.perform(withCsrf(post("/api/v1/admin/english/vocabulary/families/"+id+"/members").session(session).contentType("application/json").content("{\"spelling\":\"active\",\"partOfSpeech\":\"adj.\",\"phoneticUs\":\"/ˈæktɪv/\",\"translation\":\"积极的\",\"cefrLevel\":\"B1\"}"),fetchCsrfToken())).andExpect(status().isCreated()).andExpect(jsonPath("$.spelling").value("active"));
  mockMvc.perform(get("/api/v1/public/english/vocabulary/families/test-family-act")).andExpect(status().isOk()).andExpect(jsonPath("$.members[0].cefrLevel").value("B1"));
 }
 private MockHttpSession login()throws Exception{var r=mockMvc.perform(withCsrf(post("/api/v1/auth/login").contentType("application/json").content("{\"username\":\"family-admin\",\"password\":\"family-pass-123\"}"),fetchCsrfToken())).andExpect(status().isOk()).andReturn();return(MockHttpSession)r.getRequest().getSession(false);}
}
