# Consumer ProGuard/R8 rules for integrators of the droidshield .aar.
# See DECISIONS.md D014. Empty for now — no obfuscation-sensitive classes
# exist yet (no Dagger-generated code, no reflection-based check loading).
# Add -keep rules here as soon as reflection or Dagger codegen classes
# that must survive minification are introduced.
