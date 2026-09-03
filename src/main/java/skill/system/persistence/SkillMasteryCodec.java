package skill.system.persistence;

import skill.system.api.SkillMasteryRecord;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned payload stored inside Severed Chains' save-scoped config block. */
public final class SkillMasteryCodec {
  private static final int MAGIC = 0x534b4d31; // SKM1
  private static final int VERSION = 1;
  private static final int MAX_RECORDS = 65_535;

  private SkillMasteryCodec() { }

  public static byte[] encode(final Map<String, Map<String, SkillMasteryRecord>> records) {
    try {
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try(final DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(records.size());
        for(final var character : records.entrySet()) {
          out.writeUTF(character.getKey()); out.writeInt(character.getValue().size());
          for(final var skill : character.getValue().entrySet()) {
            out.writeUTF(skill.getKey()); out.writeInt(skill.getValue().mastery());
          }
        }
      }
      return bytes.toByteArray();
    } catch(final IOException impossible) {
      throw new IllegalStateException("Could not encode mastery data", impossible);
    }
  }

  public static Map<String, Map<String, SkillMasteryRecord>> decode(final byte[] bytes) {
    if(bytes == null || bytes.length == 0) return Map.of();
    try(final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      if(in.readInt() != MAGIC) throw new IllegalArgumentException("Not Skill Manual mastery data");
      final int version = in.readInt();
      if(version != VERSION) throw new IllegalArgumentException("Unsupported Skill Manual data version " + version);
      final int characterCount = checkedCount(in.readInt());
      final Map<String, Map<String, SkillMasteryRecord>> result = new LinkedHashMap<>();
      for(int c = 0; c < characterCount; c++) {
        final String characterId = in.readUTF();
        final int skillCount = checkedCount(in.readInt());
        final Map<String, SkillMasteryRecord> skills = new LinkedHashMap<>();
        for(int s = 0; s < skillCount; s++) {
          final String skillId = in.readUTF();
          final int mastery = in.readInt();
          if(mastery < 0) throw new IllegalArgumentException("Negative mastery in save payload");
          skills.put(skillId, new SkillMasteryRecord(mastery));
        }
        result.put(characterId, skills);
      }
      return result;
    } catch(final IOException e) {
      throw new IllegalArgumentException("Truncated Skill Manual mastery data", e);
    }
  }

  private static int checkedCount(final int count) {
    if(count < 0 || count > MAX_RECORDS) throw new IllegalArgumentException("Invalid record count " + count);
    return count;
  }
}
