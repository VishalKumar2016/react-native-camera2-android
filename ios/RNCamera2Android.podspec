
Pod::Spec.new do |s|
  s.name         = "RNCamera2Android"
  s.version      = "1.0.0"
  s.summary      = "RNCamera2Android"
  s.description  = <<-DESC
                  RNCamera2Android
                   DESC
  s.homepage     = ""
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNCamera2Android.git", :tag => "master" }
  s.source_files  = "RNCamera2Android/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  