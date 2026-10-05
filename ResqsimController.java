package com.resqsim.web;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResqsimController {
    private final JdbcTemplate db;
    public ResqsimController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/health")
    public Map<String,Object> health(){
        try { db.queryForObject("SELECT 1", Integer.class); return Map.of("ok",true,"database","connected"); }
        catch(Exception e){ return Map.of("ok",false,"database","error","message",e.getMessage()); }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,Object> b){
        String role=s(b,"role"), email=s(b,"email"), password=s(b,"password");
        if("admin".equalsIgnoreCase(role)){
            List<Map<String,Object>> rows=db.queryForList("SELECT admin_id,name,email,phone FROM ADMIN WHERE email=? AND password=?",email,password);
            if(rows.isEmpty()) return ResponseEntity.status(401).body(Map.of("success",false,"message","Invalid admin email or password"));
            var u=rows.get(0); return ResponseEntity.ok(Map.of("success",true,"role","admin","user",u));
        }
        if("victim".equalsIgnoreCase(role)){
            List<Map<String,Object>> rows=db.queryForList("SELECT victim_id,name,phone,location,emergency_type,severity,status,team_id,shelter_id,resource_request,resource_status FROM VICTIM WHERE victim_id=? AND phone=?",i(b,"victimId"),phone);
            if(rows.isEmpty()) return ResponseEntity.status(401).body(Map.of("success",false,"message","Invalid Victim ID or phone number"));
            return ResponseEntity.ok(Map.of("success",true,"role","victim","user",rows.get(0)));
        }
        return ResponseEntity.badRequest().body(Map.of("success",false,"message","Unknown role"));
    }

    @GetMapping("/dashboard") public Map<String,Object> dashboard(){
        return Map.of("victims",count("VICTIM"),"pendingSos",count("VICTIM","status='SOS Pending'"),
            "activeTeams",count("RESCUE_TEAM","rescue_status='Assigned'"),"availableTeams",count("RESCUE_TEAM","rescue_status='Available'"),
            "availableBeds",number("SELECT COALESCE(SUM(available_beds),0) FROM SHELTER_MANAGER"),"volunteers",count("VOLUNTEER"));
    }

    @GetMapping("/victims") public List<Map<String,Object>> victims(){ return db.queryForList("SELECT * FROM VICTIM ORDER BY victim_id DESC"); }
    @PostMapping("/victims") public Map<String,Object> registerVictim(@RequestBody Map<String,Object> b){
        db.update("INSERT INTO VICTIM(name,phone,status) VALUES(?,?,?)",s(b,"name"),s(b,"phone"),"Registered");
        return Map.of("message","Victim registered successfully","victimId",number("SELECT LAST_INSERT_ID()"));
    }
    @PutMapping("/victims/{id}/sos") public Map<String,Object> sos(@PathVariable int id,@RequestBody Map<String,Object> b){
        int n=db.update("UPDATE VICTIM SET location=?,emergency_type=?,severity=?,status='SOS Pending' WHERE victim_id=?",s(b,"location"),s(b,"emergencyType"),i(b,"severity"),id);
        return result(n,"SOS created successfully");
    }
    @GetMapping("/victims/{id}") public ResponseEntity<?> victim(@PathVariable int id){
        List<Map<String,Object>> r=db.queryForList("SELECT * FROM VICTIM WHERE victim_id=?",id); return r.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(r.get(0));
    }
    @PutMapping("/victims/{id}/resource") public Map<String,Object> resource(@PathVariable int id,@RequestBody Map<String,Object> b){
        return result(db.update("UPDATE VICTIM SET resource_request=?,resource_status='Resource Requested' WHERE victim_id=?",s(b,"resource"),id),"Resource request sent");
    }
    @GetMapping("/victim/{id}/full") public ResponseEntity<?> victimFull(@PathVariable int id){
        List<Map<String,Object>> r=db.queryForList("SELECT v.*,t.team_name,t.leader,t.vehicle,s.shelter_name,s.address,s.available_beds FROM VICTIM v LEFT JOIN RESCUE_TEAM t ON v.team_id=t.team_id LEFT JOIN SHELTER_MANAGER s ON v.shelter_id=s.shelter_id WHERE v.victim_id=?",id);
        return r.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(r.get(0));
    }

    @GetMapping("/shelters") public List<Map<String,Object>> shelters(){ return db.queryForList("SELECT * FROM SHELTER_MANAGER WHERE available_beds>0 ORDER BY available_beds DESC"); }
    @GetMapping("/admin/sos") public List<Map<String,Object>> sosList(){ return db.queryForList("SELECT victim_id,name,phone,location,emergency_type,severity,status,team_id FROM VICTIM WHERE status='SOS Pending' ORDER BY severity DESC,victim_id ASC"); }
    @GetMapping("/admin/resources") public List<Map<String,Object>> resourceRequests(){ return db.queryForList("SELECT victim_id,name,resource_request,resource_status FROM VICTIM WHERE resource_status='Resource Requested'"); }
    @PutMapping("/admin/resources/{id}") public Map<String,Object> allocate(@PathVariable int id){ return result(db.update("UPDATE VICTIM SET resource_status='Resources Allocated' WHERE victim_id=?",id),"Resources allocated"); }

    @GetMapping("/teams") public List<Map<String,Object>> teams(){ return db.queryForList("SELECT * FROM RESCUE_TEAM ORDER BY team_id"); }
    @GetMapping("/teams/available") public List<Map<String,Object>> availableTeams(){ return db.queryForList("SELECT * FROM RESCUE_TEAM WHERE rescue_status='Available' ORDER BY team_id"); }
    @PutMapping("/admin/assign-team") public Map<String,Object> assignTeam(@RequestBody Map<String,Object> b){
        int victimId=i(b,"victimId"), teamId=i(b,"teamId");
        try(var con=db.getDataSource().getConnection()){
            con.setAutoCommit(false);
            try(var p=con.prepareStatement("UPDATE VICTIM SET team_id=?,status='Approved' WHERE victim_id=?")){p.setInt(1,teamId);p.setInt(2,victimId);if(p.executeUpdate()==0)throw new RuntimeException("Victim not found");}
            try(var p=con.prepareStatement("UPDATE RESCUE_TEAM SET victim_id=?,rescue_status='Assigned' WHERE team_id=?")){p.setInt(1,victimId);p.setInt(2,teamId);if(p.executeUpdate()==0)throw new RuntimeException("Team not found");}
            con.commit(); return Map.of("message","Rescue team assigned successfully");
        }catch(Exception e){throw new RuntimeException(e.getMessage());}
    }

    @GetMapping("/volunteers") public List<Map<String,Object>> volunteers(){ return db.queryForList("SELECT v.*,s.shelter_name FROM VOLUNTEER v LEFT JOIN SHELTER_MANAGER s ON v.shelter_id=s.shelter_id ORDER BY v.volunteer_id DESC"); }
    @PostMapping("/volunteers") public Map<String,Object> addVolunteer(@RequestBody Map<String,Object> b){
        Integer shelter=iNullable(b,"shelterId"); db.update("INSERT INTO VOLUNTEER(name,phone,skill,shelter_id,availability) VALUES(?,?,?,?,?)",s(b,"name"),s(b,"phone"),s(b,"skill"),shelter,"Available"); return Map.of("message","Volunteer added");
    }
    @PutMapping("/volunteers/{id}/availability") public Map<String,Object> volunteerAvailability(@PathVariable int id,@RequestBody Map<String,Object> b){ return result(db.update("UPDATE VOLUNTEER SET availability=? WHERE volunteer_id=?",s(b,"availability"),id),"Availability updated"); }

    @PostMapping("/admin") public Map<String,Object> admin(@RequestBody Map<String,Object> b){ db.update("INSERT INTO ADMIN(name,email,password,phone) VALUES(?,?,?,?)",s(b,"name"),s(b,"email"),s(b,"password"),s(b,"phone")); return Map.of("message","Admin registered"); }
    @GetMapping("/admins") public List<Map<String,Object>> admins(){return db.queryForList("SELECT admin_id,name,email,phone FROM ADMIN ORDER BY admin_id DESC");}
    @PutMapping("/admin/disaster") public Map<String,Object> disaster(@RequestBody Map<String,Object> b){ return result(db.update("UPDATE VICTIM SET emergency_type=?,location=?,status='Emergency Active' WHERE victim_id=?",s(b,"emergencyType"),s(b,"location"),i(b,"victimId")),"Disaster event updated"); }
    @PutMapping("/admin/shelter") public Map<String,Object> shelter(@RequestBody Map<String,Object> b){
        int victimId=i(b,"victimId"), shelterId=i(b,"shelterId");
        int n=db.update("UPDATE VICTIM SET shelter_id=?,status='Shelter Assigned' WHERE victim_id=?",shelterId,victimId);
        if(n>0) db.update("UPDATE SHELTER_MANAGER SET available_beds=GREATEST(available_beds-1,0) WHERE shelter_id=? AND available_beds>0",shelterId);
        return result(n,"Shelter assigned");
    }
    @GetMapping("/report") public List<Map<String,Object>> report(){ return db.queryForList("SELECT v.victim_id,v.name,v.location,v.emergency_type,v.severity,v.status,v.resource_status,t.team_name,s.shelter_name FROM VICTIM v LEFT JOIN RESCUE_TEAM t ON v.team_id=t.team_id LEFT JOIN SHELTER_MANAGER s ON v.shelter_id=s.shelter_id ORDER BY v.victim_id DESC"); }

    @PostMapping("/scheduling/fcfs") public Map<String,Object> fcfs(@RequestBody List<Map<String,Object>> jobs){
        jobs.sort(Comparator.comparingInt(x->i(x,"arrival"))); int time=0; double tw=0,tt=0; List<Map<String,Object>> out=new ArrayList<>();
        for(var j:jobs){int at=i(j,"arrival"),bt=i(j,"burst");time=Math.max(time,at)+bt;int ct=time,tat=ct-at,wt=tat-bt;tw+=wt;tt+=tat;out.add(Map.of("id",s(j,"id"),"arrival",at,"burst",bt,"completion",ct,"waiting",wt,"turnaround",tat));}
        return Map.of("jobs",out,"avgWaiting",jobs.isEmpty()?0:tw/jobs.size(),"avgTurnaround",jobs.isEmpty()?0:tt/jobs.size());
    }
    @PostMapping("/scheduling/priority") public Map<String,Object> priority(@RequestBody List<Map<String,Object>> jobs){
        jobs.sort((a,b)->Integer.compare(i(b,"priority"),i(a,"priority"))); int time=0; double tw=0,tt=0; List<Map<String,Object>> out=new ArrayList<>();
        for(var j:jobs){int bt=i(j,"burst");time+=bt;int ct=time,tat=ct,wt=tat-bt;tw+=wt;tt+=tat;out.add(Map.of("id",s(j,"id"),"burst",bt,"priority",i(j,"priority"),"completion",ct,"waiting",wt,"turnaround",tat));}
        return Map.of("jobs",out,"avgWaiting",jobs.isEmpty()?0:tw/jobs.size(),"avgTurnaround",jobs.isEmpty()?0:tt/jobs.size());
    }

    private int count(String table){return number("SELECT COUNT(*) FROM "+table);} private int count(String table,String where){return number("SELECT COUNT(*) FROM "+table+" WHERE "+where);} private int number(String sql){Integer x=db.queryForObject(sql,Integer.class);return x==null?0:x;}
    private static String s(Map<String,Object>b,String k){return Objects.toString(b.get(k),"");}
    private static int i(Map<String,Object>b,String k){Object v=b.get(k);return v instanceof Number?((Number)v).intValue():Integer.parseInt(Objects.toString(v,"0"));}
    private static Integer iNullable(Map<String,Object>b,String k){Object v=b.get(k); if(v==null||Objects.toString(v,"").isBlank()||"0".equals(Objects.toString(v))) return null; return i(b,k);}
    private static Map<String,Object> result(int n,String msg){return n>0?Map.of("success",true,"message",msg):Map.of("success",false,"message","Record not found");}
}
