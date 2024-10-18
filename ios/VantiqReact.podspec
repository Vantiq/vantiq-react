Pod::Spec.new do |s|
  s.name             = "VantiqReact"
  s.version          = "1.0.0"
  s.summary          = "A React Native package that utilizes the Vantiq UI libraries"
  s.homepage         = "https://github.com/Vantiq/vantiq-react"
  s.license          = { :type => "MIT", :file => "LICENSE" }
  s.authors          = { "Vantiq, Inc." => "support@vantiq.com" }
  s.source           = { :git => "https://github.com/Vantiq/vantiq-react.git", :tag => "1.0.0" }
  s.platform         = :ios, '13.0'
  s.source_files     = 'VantiqReact/*.{h,m,mm,swift}'
  s.dependency       = 'vantiq-ui-ios', '~> 0.2.9', :inhibit_warnings => true
  # s.frameworks       = ['UIKit']
end
