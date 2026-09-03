package skill.system.steal;

/** null resource means this instance failed its initial 50% held-resource roll. */
public final class HeldResourceState {
  private final HeldResource resource;
  private boolean stolen;

  public HeldResourceState(final HeldResource resource) { this.resource = resource; }
  public HeldResource resource() { return this.resource; }
  public boolean hasAvailableResource() { return this.resource != null && !this.stolen; }
  public boolean stolen() { return this.stolen; }
  public void markStolen() { this.stolen = true; }
}
