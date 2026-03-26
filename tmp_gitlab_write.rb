require 'json'
project = Project.find_by_full_path('root/rocksdb')
user = User.find_by_username('root')
token = "codex-int-#{Time.now.to_i}"
issue = project.issues.create!(title: "#{token} issue", description: "#{token} description", author: user)
label = project.labels.create!(title: "#{token}-label")
issue.labels << label
note = issue.notes.create!(note: "#{token} note", author: user, project: project)
puts({token: token, issue_id: issue.id, note_id: note.id, label_id: label.id}.to_json)
