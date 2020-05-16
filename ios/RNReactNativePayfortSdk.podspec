
Pod::Spec.new do |s|
  s.name         = "RNReactNativePayfortSdk"
  s.version      = "1.0.0"
  s.summary      = "RNReactNativePayfortSdk"
  s.description  = <<-DESC
                  RNReactNativePayfortSdk
                   DESC
  s.homepage     = "https://github.com/Simicart/RN-Payfort-SDK#readme"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNReactNativePayfortSdk.git", :tag => "master" }
  s.source_files  = "RNReactNativePayfortSdk/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  