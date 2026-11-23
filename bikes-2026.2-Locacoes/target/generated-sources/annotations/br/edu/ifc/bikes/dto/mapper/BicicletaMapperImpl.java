package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.StatusBicicleta;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T16:53:29-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class BicicletaMapperImpl implements BicicletaMapper {

    @Override
    public Bicicleta toEntity(BicicletaRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Bicicleta bicicleta = new Bicicleta();

        bicicleta.setCodigo( dto.codigo() );
        bicicleta.setModelo( dto.modelo() );

        return bicicleta;
    }

    @Override
    public BicicletaResponseDTO toResponse(Bicicleta bicicleta) {
        if ( bicicleta == null ) {
            return null;
        }

        Long id = null;
        String codigo = null;
        String modelo = null;
        StatusBicicleta status = null;

        id = bicicleta.getId();
        codigo = bicicleta.getCodigo();
        modelo = bicicleta.getModelo();
        status = bicicleta.getStatus();

        BicicletaResponseDTO bicicletaResponseDTO = new BicicletaResponseDTO( id, codigo, modelo, status );

        return bicicletaResponseDTO;
    }

    @Override
    public List<BicicletaResponseDTO> toResponse(List<Bicicleta> bicicletas) {
        if ( bicicletas == null ) {
            return null;
        }

        List<BicicletaResponseDTO> list = new ArrayList<BicicletaResponseDTO>( bicicletas.size() );
        for ( Bicicleta bicicleta : bicicletas ) {
            list.add( toResponse( bicicleta ) );
        }

        return list;
    }

    @Override
    public void updateEntity(BicicletaRequestDTO dto, Bicicleta bicicleta) {
        if ( dto == null ) {
            return;
        }

        bicicleta.setCodigo( dto.codigo() );
        bicicleta.setModelo( dto.modelo() );
    }
}
