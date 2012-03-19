Name:		jon-setup	
Version:	1.0
Release:	1%{?dist}
Summary:	CLI tool to set up JON database and trigger install from within a script

Group:		Administration
License:	LGPL	
URL:		http://belaran.eu/		
Source0:	%{name}-%{version}.tgz
BuildRoot:	%(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
Packager:	Romain Pelisse
BuildArch:	noarch

BuildRequires:	/bin/rm, /bin/rmdir, /bin/cp
Requires:	java 

%description
handy script

%prep
%setup -q


%build
#configure
#make %{?_smp_mflags}
mkdir -p $RPM_BUILD_ROOT/usr/local/java/lib/
mkdir -p $RPM_BUILD_ROOT/usr/local/bin/
cp jon-setup-%{version}.jar $RPM_BUILD_ROOT/usr/local/java/lib/
cp jon-setup $RPM_BUILD_ROOT/usr/local/bin

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p %{buildroot}/usr/local/bin/
mkdir -p %{buildroot}/usr/local/java/lib/  
cp -p jon-setup %{buildroot}/usr/local/bin/
cp -p jon-setup-%{version}.jar %{buildroot}/usr/local/java/lib/

%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%attr(0755,root,root) /usr/local/bin/jon-setup
/usr/local/java/lib/jon-setup-1.0.jar

%changelog
* Mon Mar 19 2012 Romain Pelisse <belaran@gmail.com> 1.0-1
- Initial RPM
