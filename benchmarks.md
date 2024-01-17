## Benchmarks

# Culling Dead Rays

- Lambertian Diffuse, 3 bounces: ~43 ms peak frametime
- Mirror, 3 bounces: ~27 ms peak frametime

# Dead Rays set to 0 Velocity

- Lambertian Diffuse, 3 bounces: ~52 ms peak frametime
- Mirror, 3 bounces: ~31 ms peak frametime

# Culling Dead Rays, 1 normal per leaf

- Mirror, 3 bounces: ~36 ms peak frametime

# Per-voxel lighting scheme

- Use pointer buffer to create Hash Map of unique voxel pointers
- Iterate thru hash map; for each voxel calculate lighting and re-iterate thru pointer buffer, fill in corresponding values

  23.5MB
  27.19MB
