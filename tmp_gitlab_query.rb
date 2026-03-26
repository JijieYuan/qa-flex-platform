project = Project.find_by_full_path('root/rocksdb')
puts(project ? project.id : 'NONE')
